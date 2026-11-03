package application.services;

import application.adapters.out.memory.InMemoryInventoryRepositoryAdapter;
import application.adapters.out.memory.InMemoryOrderRepositoryAdapter;
import application.adapters.out.memory.InMemoryProductRepositoryAdapter;
import application.domain.enums.OrderStatus;
import application.domain.enums.ProductStatus;
import application.domain.enums.ProductType;
import application.domain.enums.ReturnStatus;
import application.domain.models.Inventory;
import application.domain.models.InventoryAllocation;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.ReturnRequest;
import application.domain.ports.in.OrderInputPort;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.ReturnRequestRepositoryPort;
import application.services.billing.InvoiceService;
import application.services.inventory.InventoryService;
import application.services.order.OrderService;
import application.services.product.ProductService;
import application.services.reports.AdministrativeReportService;
import application.services.returns.ReturnService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BusinessServicesTest {
    @Test
    void invoiceCreationIsIdempotentForPaidOrders() {
        InvoiceRepositoryPort repository = mock(InvoiceRepositoryPort.class);
        Map<Long, application.domain.models.Invoice> saved = new HashMap<>();
        when(repository.findByOrderId(anyLong())).thenAnswer(invocation ->
                Optional.ofNullable(saved.get(invocation.getArgument(0))));
        when(repository.save(any())).thenAnswer(invocation -> {
            application.domain.models.Invoice invoice = invocation.getArgument(0);
            saved.put(invoice.orderId(), invoice);
            return invoice;
        });
        InvoiceService service = new InvoiceService(repository);

        Order order = new Order(1L, 2L, OrderStatus.CART);
        order.addProduct(3L, 2, new BigDecimal("5.00"));
        order.sendToPayment();
        order.markAsPaid();
        var first = service.createForPaidOrder(order);
        var second = service.createForPaidOrder(order);

        assertEquals(first.id(), second.id());
        assertEquals(new BigDecimal("10.00"), first.total());
        assertThrows(IllegalStateException.class, () -> service.createForPaidOrder(
                new Order(4L, 2L, OrderStatus.CART)));
    }

    @Test
    void approvedReturnRestocksInventoryAndRefundIsRecordedManually() {
        InventoryService inventory = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        inventory.register(new Inventory(10L, 3L, 4L, 1));
        inventory.register(new Inventory(11L, 3L, 5L, 9));
        List<InventoryAllocation> allocations = inventory.reserveByProduct(3L, 2);

        Order order = new Order(1L, 2L, OrderStatus.CART);
        order.addProduct(3L, 2, new BigDecimal("5.00"), allocations);
        order.sendToPayment();
        order.markAsPaid();
        order.dispatch();
        order.finalizeOrder();
        OrderInputPort orders = mock(OrderInputPort.class);
        when(orders.findById(1L)).thenReturn(order);

        ReturnRequestRepositoryPort repository = mock(ReturnRequestRepositoryPort.class);
        Map<String, ReturnRequest> saved = new HashMap<>();
        Map<Long, Integer> requestedQuantities = new HashMap<>();
        when(repository.findByOrderId(1L)).thenAnswer(invocation -> List.copyOf(saved.values()));
        when(repository.findById(anyString())).thenAnswer(invocation ->
                Optional.ofNullable(saved.get(invocation.getArgument(0))));
        when(repository.save(any())).thenAnswer(invocation -> {
            ReturnRequest request = invocation.getArgument(0);
            saved.put(request.getId(), request);
            return request;
        });
        when(repository.transitionStatus(anyString(), any(), any())).thenAnswer(invocation -> {
            ReturnRequest current = saved.get(invocation.getArgument(0));
            if (current == null || current.getStatus() != invocation.getArgument(1)) return false;
            ReturnRequest transitioned = current.copy();
            ReturnStatus next = invocation.getArgument(2);
            if (next == ReturnStatus.APPROVED) transitioned.approve();
            else if (next == ReturnStatus.REJECTED) transitioned.reject();
            else transitioned.recordRefund();
            saved.put(transitioned.getId(), transitioned);
            return true;
        });
        when(orders.reserveReturnQuantity(anyLong(), anyLong(), any(Integer.class),
                any(Integer.class), any(Integer.class)))
                .thenAnswer(invocation -> {
                    Long productId = invocation.getArgument(1);
                    int purchased = invocation.getArgument(2);
                    int baseline = invocation.getArgument(3);
                    int quantity = invocation.getArgument(4);
                    int previous = requestedQuantities.getOrDefault(productId, baseline);
                    if (quantity > purchased - previous) {
                        throw new IllegalArgumentException("Return quantity exceeds the unreturned purchased quantity");
                    }
                    requestedQuantities.put(productId, previous + quantity);
                    return previous;
                });
        org.mockito.Mockito.doAnswer(invocation -> {
            Long productId = invocation.getArgument(1);
            int quantity = invocation.getArgument(2);
            requestedQuantities.compute(productId, (key, current) -> current - quantity);
            return null;
        }).when(orders).releaseReturnQuantity(anyLong(), anyLong(), org.mockito.ArgumentMatchers.anyInt(), anyString());

        ReturnService service = new ReturnService(repository, orders, inventory);
        ReturnRequest request = service.request(1L, 3L, 1, "Unwanted item");
        ReturnRequest secondRequest = service.request(1L, 3L, 1, "Second item");
        assertEquals(List.of(new InventoryAllocation(10L, 1)), request.getAllocations());
        assertEquals(List.of(new InventoryAllocation(11L, 1)), secondRequest.getAllocations());
        assertThrows(IllegalArgumentException.class,
                () -> service.request(1L, 3L, 1, "Exceeds purchased quantity"));

        service.approve(request.getId());
        assertEquals(1, inventory.getAvailability(10L));
        assertEquals(8, inventory.getAvailability(11L));
        service.approve(request.getId());
        assertEquals(1, inventory.getAvailability(10L));
        assertEquals(8, inventory.getAvailability(11L));
        service.recordRefund(request.getId());
        assertEquals(ReturnStatus.REFUNDED, service.findById(request.getId()).getStatus());
        service.reject(secondRequest.getId());
        ReturnRequest replacementRequest = service.request(1L, 3L, 1, "Replacement request");
        assertEquals(List.of(new InventoryAllocation(11L, 1)), replacementRequest.getAllocations());
        service.approve(replacementRequest.getId());
        assertEquals(9, inventory.getAvailability(11L));
    }

    @Test
    void summaryReportsCurrentStatusesSalesAndDamagedStock() {
        InventoryService inventory = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        inventory.register(new Inventory(10L, 3L, 4L, 7));
        inventory.markDamaged(10L, 2);
        ProductService products = new ProductService(new InMemoryProductRepositoryAdapter());
        products.register(new Product(3L, 7L, "Test product", "Report item", ProductType.PHYSICAL,
                List.of(), ProductStatus.PUBLISHED, BigDecimal.TEN));
        OrderService orders = new OrderService(new InMemoryOrderRepositoryAdapter(), inventory, products);
        orders.create(1L, 2L);
        orders.addProduct(1L, 3L, 1);
        orders.sendToPayment(1L);
        orders.markAsPaid(1L);

        var report = new AdministrativeReportService(orders, inventory).summary();

        assertEquals(1L, report.ordersByStatus().get(OrderStatus.PAID));
        assertEquals(BigDecimal.TEN, report.grossSales());
        assertEquals(4, report.availableInventory());
        assertEquals(2, report.damagedInventory());
    }
}
