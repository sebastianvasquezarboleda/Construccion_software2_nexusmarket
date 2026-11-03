package application.services;

import application.adapters.out.memory.InMemoryInventoryRepositoryAdapter;
import application.adapters.out.memory.InMemoryOrderRepositoryAdapter;
import application.adapters.out.memory.InMemoryProductRepositoryAdapter;
import application.adapters.out.memory.InMemoryUserRepositoryAdapter;
import application.domain.enums.ProductStatus;
import application.domain.enums.ProductType;
import application.domain.enums.Role;
import application.domain.enums.UserStatus;
import application.domain.models.Inventory;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.User;
import application.domain.ports.out.OrderRepositoryPort;
import application.services.inventory.InventoryService;
import application.services.order.OrderService;
import application.services.product.ProductService;
import application.services.user.UserService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ServicesTest {

    @Test
    void userServiceRegistersAndChangesStatus() {
        var repository = new InMemoryUserRepositoryAdapter();
        var encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        UserService service = new UserService(repository, encoder);
        service.register(new User(1L, "Ana", "ana@example.com", "123", Role.BUYER, UserStatus.BLOCKED),
                "BuyerTestPass123!");

        service.activate(1L);

        assertTrue(service.isActive(1L));
        assertThrows(IllegalArgumentException.class, () -> service.findById(99L));
        var credential = repository.findCredentialByEmail("ANA@example.com").orElseThrow();
        assertTrue(!credential.passwordHash().equals("BuyerTestPass123!"));
        assertTrue(encoder.matches("BuyerTestPass123!", credential.passwordHash()));
    }

    @Test
    void userServiceRejectsDuplicateEmailAndIdentityDocumentIgnoringCase() {
        UserService service = new UserService(new InMemoryUserRepositoryAdapter(),
                new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder());
        service.register(new User(1L, "Ana", "ana@example.com", "doc-123", Role.BUYER, UserStatus.ACTIVE),
                "BuyerTestPass123!");

        assertThrows(IllegalArgumentException.class, () -> service.register(
                new User(2L, "Other", "ANA@EXAMPLE.COM", "DOC-456", Role.BUYER, UserStatus.ACTIVE),
                "OtherTestPass123!"));
        assertThrows(IllegalArgumentException.class, () -> service.register(
                new User(4L, "Other", "  ana@example.com  ", "DOC-789", Role.BUYER, UserStatus.ACTIVE),
                "OtherTestPass123!"));
        assertThrows(IllegalArgumentException.class, () -> service.register(
                new User(3L, "Other", "other@example.com", "DOC-123", Role.BUYER, UserStatus.ACTIVE),
                "OtherTestPass123!"));
    }

    @Test
    void productServiceDelegatesProductLifecycle() {
        ProductService service = new ProductService(new InMemoryProductRepositoryAdapter());
        service.register(new Product(1L, 10L, "Keyboard", "Mechanical keyboard", ProductType.PHYSICAL,
                List.of(), ProductStatus.SUSPENDED));

        service.addVariant(1L, "US layout");
        service.publish(1L);

        assertEquals(ProductStatus.PUBLISHED, service.findById(1L).getStatus());
        assertEquals(List.of("US layout"), service.findById(1L).getVariants());
    }

    @Test
    void inventoryServiceDelegatesAvailabilityRules() {
        InventoryService service = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        service.register(new Inventory(1L, 10L, 20L, 5));

        service.reserve(1L, 2);

        assertEquals(3, service.getAvailability(1L));
        assertThrows(IllegalStateException.class, () -> service.reserve(1L, 4));
    }

    @Test
    void damagedInventoryIsUnavailableForReservation() {
        InventoryService service = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        service.register(new Inventory(2L, 12L, 20L, 5));

        service.markDamaged(2L, 2);

        assertEquals(3, service.getAvailability(2L));
        assertEquals(2, service.findById(2L).getDamagedQuantity());
        assertThrows(IllegalStateException.class, () -> service.reserve(2L, 4));
    }

    @Test
    void orderServiceReservesInventoryWhenAddingProducts() {
        InventoryService inventoryService = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        inventoryService.register(new Inventory(100L, 55L, 77L, 10));
        OrderService service = orderService(inventoryService, 55L, new BigDecimal("7.50"));

        service.create(9L, 3L);
        service.addProduct(9L, 55L, 4);

        assertEquals(6, inventoryService.getAvailability(100L));
        assertEquals(new BigDecimal("30.00"), service.findById(9L).getTotal());
    }

    @Test
    void orderReservationsSpanWarehousesAndRestoreTheSameAllocations() {
        InventoryService inventoryService = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        inventoryService.register(new Inventory(101L, 55L, 77L, 5));
        inventoryService.register(new Inventory(102L, 55L, 78L, 7));
        OrderService service = orderService(inventoryService, 55L, new BigDecimal("7.50"));

        service.create(10L, 3L);
        service.addProduct(10L, 55L, 8);

        var item = service.findById(10L).getItems().get(0);
        assertEquals(List.of(
                new application.domain.models.InventoryAllocation(101L, 5),
                new application.domain.models.InventoryAllocation(102L, 3)), item.allocations());
        assertEquals(0, inventoryService.getAvailability(101L));
        assertEquals(4, inventoryService.getAvailability(102L));

        service.removeProduct(10L, 55L);

        assertEquals(5, inventoryService.getAvailability(101L));
        assertEquals(7, inventoryService.getAvailability(102L));
    }

    @Test
    void digitalOrderDoesNotRequirePhysicalInventory() {
        InventoryService inventoryService = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        ProductService products = new ProductService(new InMemoryProductRepositoryAdapter());
        products.register(new Product(66L, 7L, "E-book", "Digital download", ProductType.DIGITAL,
                List.of(), ProductStatus.PUBLISHED, new BigDecimal("9.99")));
        OrderService service = new OrderService(new InMemoryOrderRepositoryAdapter(), inventoryService, products);

        service.create(67L, 3L);
        service.addProduct(67L, 66L, 1);

        assertEquals(0, inventoryService.findAll().size());
        assertEquals(new BigDecimal("9.99"), service.findById(67L).getTotal());
    }

    @Test
    void invalidOrderProductDoesNotReserveInventory() {
        InventoryService inventoryService = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        inventoryService.register(new Inventory(100L, 55L, 77L, 10));
        OrderService service = orderService(inventoryService, 55L, BigDecimal.ONE);
        service.create(9L, 3L);

        assertThrows(IllegalArgumentException.class,
                () -> service.addProduct(9L, 55L, 0));
        assertThrows(IllegalArgumentException.class,
                () -> service.addProduct(9L, 999L, 2));
        assertEquals(10, inventoryService.getAvailability(100L));
    }

    @Test
    void removingProductReturnsStockForAllMatchingOrderLines() {
        InventoryService inventoryService = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        inventoryService.register(new Inventory(100L, 55L, 77L, 10));
        OrderService service = orderService(inventoryService, 55L, BigDecimal.ONE);
        service.create(9L, 3L);
        service.addProduct(9L, 55L, 2);
        service.addProduct(9L, 55L, 3);

        service.removeProduct(9L, 55L);

        assertEquals(10, inventoryService.getAvailability(100L));
        assertTrue(service.findById(9L).getItems().isEmpty());
    }

    @Test
    void cancellingFinalizedOrderDoesNotReturnStock() {
        InventoryService inventoryService = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        inventoryService.register(new Inventory(100L, 55L, 77L, 10));
        OrderService service = orderService(inventoryService, 55L, BigDecimal.ONE);
        service.create(9L, 3L);
        service.addProduct(9L, 55L, 2);
        service.sendToPayment(9L);
        service.markAsPaid(9L);
        service.dispatch(9L);
        service.finalizeOrder(9L);

        assertThrows(IllegalStateException.class, () -> service.cancel(9L));
        assertEquals(8, inventoryService.getAvailability(100L));
    }

    @Test
    void cancelledOrderIsTerminal() {
        InventoryService inventoryService = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        inventoryService.register(new Inventory(100L, 55L, 77L, 10));
        OrderService service = orderService(inventoryService, 55L, BigDecimal.ONE);
        service.create(9L, 3L);
        service.cancel(9L);

        assertEquals(application.domain.enums.OrderStatus.CANCELLED, service.findById(9L).getStatus());
        assertThrows(IllegalStateException.class,
                () -> service.addProduct(9L, 55L, 1));
    }

    @Test
    void failedOrderSaveCompensatesStockAndKeepsStoredOrderUnchanged() {
        InventoryService inventoryService = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        inventoryService.register(new Inventory(100L, 55L, 77L, 10));
        OrderRepositoryPort orders = mock(OrderRepositoryPort.class);
        Order storedOrder = new Order(9L, 3L, application.domain.enums.OrderStatus.CART);
        when(orders.findById(9L)).thenReturn(Optional.of(storedOrder));
        doThrow(new IllegalStateException("save failed")).when(orders).save(any(Order.class));
        OrderService service = new OrderService(orders, inventoryService,
                productService(55L, BigDecimal.ONE));

        assertThrows(IllegalStateException.class,
                () -> service.addProduct(9L, 55L, 2));

        assertEquals(10, inventoryService.getAvailability(100L));
        assertTrue(storedOrder.getItems().isEmpty());
    }

    @Test
    void orderServiceCancelsOrderAndReturnsStock() {
        InventoryService inventoryService = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        inventoryService.register(new Inventory(100L, 55L, 77L, 10));
        OrderService service = orderService(inventoryService, 55L, new BigDecimal("7.50"));

        service.create(9L, 3L);
        service.addProduct(9L, 55L, 4);
        service.cancel(9L);

        assertEquals(10, inventoryService.getAvailability(100L));
        assertTrue(service.findById(9L).getItems().isEmpty());
    }

    @Test
    void orderServiceCoordinatesOrderLifecycle() {
        InventoryService inventoryService = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        inventoryService.register(new Inventory(100L, 10L, 20L, 10));
        OrderService service = orderService(inventoryService, 10L, new BigDecimal("12.50"));
        service.create(1L, 2L);
        service.addProduct(1L, 10L, 2);
        service.sendToPayment(1L);
        service.markAsPaid(1L);
        service.dispatch(1L);
        service.finalizeOrder(1L);

        assertEquals(new BigDecimal("25.00"), service.findById(1L).getTotal());
    }

    @Test
    void onlyUnpaidOrdersCanBeCancelled() {
        InventoryService inventoryService = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        inventoryService.register(new Inventory(200L, 55L, 77L, 10));
        OrderService service = orderService(inventoryService, 55L, BigDecimal.ONE);
        service.create(201L, 3L);
        service.addProduct(201L, 55L, 2);
        service.sendToPayment(201L);
        service.markAsPaid(201L);

        assertThrows(IllegalStateException.class, () -> service.cancel(201L));
        assertEquals(8, inventoryService.getAvailability(200L));
    }

    @Test
    void expirationCancelsOldUnpaidOrderAndReturnsReservedStock() {
        InventoryService inventoryService = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        inventoryService.register(new Inventory(210L, 55L, 77L, 10));
        OrderService service = orderService(inventoryService, 55L, BigDecimal.ONE);
        service.create(211L, 3L);
        service.addProduct(211L, 55L, 4);
        service.sendToPayment(211L);

        int expired = service.expireAbandonedOrders(Instant.now().plusSeconds(1));

        assertEquals(1, expired);
        assertEquals(10, inventoryService.getAvailability(210L));
        assertEquals(application.domain.enums.OrderStatus.CANCELLED, service.findById(211L).getStatus());
    }

    @Test
    void expirationLeavesRecentAndPaidOrdersUntouched() {
        InventoryService inventoryService = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        inventoryService.register(new Inventory(220L, 55L, 77L, 10));
        OrderService service = orderService(inventoryService, 55L, BigDecimal.ONE);
        service.create(221L, 3L);
        service.addProduct(221L, 55L, 2);
        service.create(222L, 3L);
        service.addProduct(222L, 55L, 2);
        service.sendToPayment(222L);
        service.markAsPaid(222L);

        int expired = service.expireAbandonedOrders(Instant.now().minusSeconds(1));

        assertEquals(0, expired);
        assertEquals(6, inventoryService.getAvailability(220L));
        assertEquals(application.domain.enums.OrderStatus.CART, service.findById(221L).getStatus());
        assertEquals(application.domain.enums.OrderStatus.PAID, service.findById(222L).getStatus());
    }

    @Test
    void staleOrderVersionCannotOverwriteConcurrentOrderUpdate() {
        InMemoryOrderRepositoryAdapter orders = new InMemoryOrderRepositoryAdapter();
        Order initial = new Order(223L, 3L, application.domain.enums.OrderStatus.CART);
        orders.save(initial);
        Order stale = orders.findById(223L).orElseThrow().copy();
        Order current = orders.findById(223L).orElseThrow().copy();
        current.updateTotal();
        orders.save(current);
        stale.cancel();

        assertThrows(org.springframework.dao.OptimisticLockingFailureException.class,
                () -> orders.save(stale));
        assertEquals(application.domain.enums.OrderStatus.CART,
                orders.findById(223L).orElseThrow().getStatus());
    }

    @Test
    void saleConfirmationIsRecordedOnceWithoutChangingReservedQuantity() {
        InventoryService inventory = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        inventory.register(new Inventory(300L, 55L, 77L, 5));
        var allocations = inventory.reserveByProduct(55L, 2);

        inventory.confirmSaleAllocations(301L, allocations);
        inventory.confirmSaleAllocations(301L, allocations);

        var saved = inventory.findById(300L);
        assertEquals(3, saved.getQuantity());
        assertEquals(2, saved.getMovements().stream()
                .filter(movement -> movement.type()
                        == application.domain.enums.InventoryMovementType.SALE_OUT)
                .mapToInt(application.domain.models.InventoryMovement::quantity).sum());
    }

    @Test
    void addToOrderUsesCatalogPriceAndRejectsUnpublishedProducts() {
        InventoryService inventory = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        inventory.register(new Inventory(310L, 55L, 77L, 5));
        ProductService products = new ProductService(new InMemoryProductRepositoryAdapter());
        products.register(new Product(55L, 999L, "Listed item", "A test item", ProductType.PHYSICAL,
                List.of(), ProductStatus.PUBLISHED, new BigDecimal("14.25")));
        OrderService orders = new OrderService(new InMemoryOrderRepositoryAdapter(), inventory, products);
        orders.create(311L, 3L);
        orders.addProduct(311L, 55L, 2);

        assertEquals(new BigDecimal("28.50"), orders.findById(311L).getTotal());
        products.suspend(55L);
        orders.create(312L, 3L);

        assertThrows(IllegalStateException.class, () -> orders.addProduct(312L, 55L, 1));
        assertEquals(3, inventory.getAvailability(310L));
    }

    @Test
    void staleInventorySnapshotCannotOverwriteAConcurrentReservation() {
        var repository = new InMemoryInventoryRepositoryAdapter();
        repository.save(new Inventory(320L, 55L, 77L, 10));
        Inventory firstSnapshot = repository.findById(320L).orElseThrow();
        Inventory staleSnapshot = repository.findById(320L).orElseThrow();
        Inventory firstUpdate = copyAndReserve(firstSnapshot, 7);
        Inventory staleUpdate = copyAndReserve(staleSnapshot, 7);

        assertTrue(repository.saveIfVersionMatches(firstSnapshot, firstUpdate));
        assertTrue(!repository.saveIfVersionMatches(staleSnapshot, staleUpdate));
        assertEquals(3, repository.findById(320L).orElseThrow().getQuantity());
    }

    private static Inventory copyAndReserve(Inventory inventory, int quantity) {
        Inventory copy = new Inventory(inventory.getId(), inventory.getProductId(), inventory.getWarehouseId(),
                inventory.getQuantity(), inventory.getDamagedQuantity(), inventory.getMovements(),
                inventory.getVersion());
        copy.reserve(quantity);
        return copy;
    }

    private static OrderService orderService(InventoryService inventory, Long productId, BigDecimal price) {
        return new OrderService(new InMemoryOrderRepositoryAdapter(), inventory, productService(productId, price));
    }

    private static ProductService productService(Long productId, BigDecimal price) {
        ProductService products = new ProductService(new InMemoryProductRepositoryAdapter());
        products.register(new Product(productId, 999L, "Test product", "Product used by service test",
                ProductType.PHYSICAL, List.of(), ProductStatus.PUBLISHED, price));
        return products;
    }
}