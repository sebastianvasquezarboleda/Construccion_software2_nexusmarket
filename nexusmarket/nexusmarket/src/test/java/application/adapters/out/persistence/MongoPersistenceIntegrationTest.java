package application.adapters.out.persistence;

import application.domain.enums.OrderStatus;
import application.domain.enums.InventoryMovementType;
import application.domain.enums.ProductStatus;
import application.domain.enums.ProductType;
import application.domain.enums.ReturnStatus;
import application.domain.enums.Role;
import application.domain.enums.ShipmentStatus;
import application.domain.enums.UserStatus;
import application.domain.enums.WarehouseType;
import application.domain.models.Buyer;
import application.domain.models.Invoice;
import application.domain.models.InvoiceLine;
import application.domain.models.Inventory;
import application.domain.models.InventoryAllocation;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.ReturnRequest;
import application.domain.models.Seller;
import application.domain.models.Shipment;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.out.BuyerRepositoryPort;
import application.domain.ports.out.InvoiceRepositoryPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.ReturnRequestRepositoryPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@EnabledIfSystemProperty(named = "mongodb.integration", matches = "true")
class MongoPersistenceIntegrationTest {
    @Autowired
    private UserRepositoryPort users;

    @Autowired
    private BuyerRepositoryPort buyers;

    @Autowired
    private SellerRepositoryPort sellers;

    @Autowired
    private WarehouseRepositoryPort warehouses;

    @Autowired
    private ProductRepositoryPort products;

    @Autowired
    private InventoryRepositoryPort inventories;

    @Autowired
    private OrderRepositoryPort orders;

    @Autowired
    private InvoiceRepositoryPort invoices;

    @Autowired
    private ShipmentRepositoryPort shipments;

    @Autowired
    private ReturnRequestRepositoryPort returns;

    @Test
    void allPortsPersistAndRestoreTheirDomainObjects() throws Exception {
        User user = new User(951L, "Mongo Test", "mongo.test@example.com", "DOC-951",
                Role.BUYER, UserStatus.ACTIVE);
        users.save(user);
        assertEquals(user.getEmail(), users.findById(951L).orElseThrow().getEmail());

        Buyer buyer = new Buyer(952L, 951L, "Main address", List.of("Additional address"), "ACTIVE");
        buyers.save(buyer);
        assertEquals(List.of("Additional address"),
                buyers.findById(952L).orElseThrow().getAdditionalAddresses());

        Seller seller = new Seller(953L, 951L);
        sellers.save(seller);
        assertEquals(953L, sellers.findByUserId(951L).orElseThrow().id());

        Warehouse warehouse = new Warehouse(954L, "Mongo test warehouse", WarehouseType.MARKETPLACE);
        warehouses.save(warehouse);
        assertEquals(warehouse.type(), warehouses.findById(954L).orElseThrow().type());

        Product product = new Product(955L, 958L, "Mongo test product", "Description",
                ProductType.PHYSICAL, List.of("Blue"), ProductStatus.PUBLISHED, new BigDecimal("12.50"));
        products.save(product);
        assertEquals(List.of("Blue"), products.findById(955L).orElseThrow().getVariants());
        assertEquals(new BigDecimal("12.50"), products.findById(955L).orElseThrow().getPrice());

        Inventory inventory = new Inventory(956L, 955L, 954L, 8);
        inventory.reserve(2);
        inventory.markDamaged(1);
        inventories.save(inventory);
        Inventory restoredInventory = inventories.findById(956L).orElseThrow();
        assertEquals(5, restoredInventory.getAvailability());
        assertEquals(1, restoredInventory.getDamagedQuantity());
        assertEquals(List.of(InventoryMovementType.ENTRY, InventoryMovementType.RESERVATION),
                restoredInventory.getMovements().stream().map(movement -> movement.type()).toList().subList(0, 2));
        assertEquals(InventoryMovementType.DAMAGED,
                restoredInventory.getMovements().get(restoredInventory.getMovements().size() - 1).type());

        Order order = new Order(957L, 952L, OrderStatus.CART);
        order.addProduct(955L, 2, new BigDecimal("12.50"), List.of(new InventoryAllocation(956L, 2)));
        order.sendToPayment();
        order.markAsPaid();
        orders.findById(957L).ifPresent(existing -> order.setVersion(existing.getVersion()));
        orders.save(order);

        Order restored = orders.findById(957L).orElseThrow();
        assertEquals(OrderStatus.PAID, restored.getStatus());
        assertEquals(1, restored.getItems().size());
        assertEquals(List.of(new InventoryAllocation(956L, 2)), restored.getItems().get(0).allocations());
        assertEquals(0, new BigDecimal("25.00").compareTo(restored.getTotal()));

        Order concurrentUpdate = restored.copy();
        concurrentUpdate.updateTotal();
        orders.save(concurrentUpdate);
        assertThrows(org.springframework.dao.OptimisticLockingFailureException.class,
                () -> orders.save(restored));

        Instant staleTimestamp = Instant.now().minusSeconds(3600);
        Order abandonedCart = new Order(1_000_000_000_000L + System.currentTimeMillis(),
                952L, OrderStatus.CART, staleTimestamp, staleTimestamp, null);
        abandonedCart.addProduct(955L, 1, new BigDecimal("12.50"),
                List.of(new InventoryAllocation(956L, 1)));
        abandonedCart.restoreUpdatedAt(staleTimestamp);
        orders.save(abandonedCart);
        assertTrue(orders.findExpiredUnpaid(Instant.now().minusSeconds(1800)).stream()
                .anyMatch(candidate -> candidate.getId().equals(abandonedCart.getId())));

        Invoice invoice = new Invoice("mongo-test-invoice-957", 957L, 952L,
                List.of(new InvoiceLine(955L, 2, new BigDecimal("12.50"))),
                new BigDecimal("25.00"), Instant.now());
        invoices.save(invoice);
        assertEquals(invoice.total(), invoices.findByOrderId(957L).orElseThrow().total());

        Shipment shipment = new Shipment("mongo-test-shipment-957", 957L, "Test address", ShipmentStatus.CREATED);
        shipments.save(shipment);
        assertEquals(ShipmentStatus.CREATED, shipments.findByOrderId(957L).orElseThrow().getStatus());

        ReturnRequest request = new ReturnRequest("mongo-test-return-957", 957L, 952L, 955L, 1,
                "Test return", ReturnStatus.REQUESTED, Instant.now(),
                List.of(new InventoryAllocation(956L, 1)));
        returns.save(request);
        ReturnRequest restoredReturn = returns.findById(request.getId()).orElseThrow();
        assertEquals(ReturnStatus.REQUESTED, restoredReturn.getStatus());
        assertEquals(List.of(new InventoryAllocation(956L, 1)), restoredReturn.getAllocations());
        assertTrue(returns.transitionStatus(request.getId(), ReturnStatus.REQUESTED, ReturnStatus.APPROVED));
        assertFalse(returns.transitionStatus(request.getId(), ReturnStatus.REQUESTED, ReturnStatus.APPROVED));

        Long returnableOrderId = 900_000_000_000L + System.currentTimeMillis();
        Order returnableOrder = new Order(returnableOrderId, 952L, OrderStatus.CART);
        returnableOrder.addProduct(955L, 2, new BigDecimal("12.50"));
        returnableOrder.sendToPayment();
        returnableOrder.markAsPaid();
        returnableOrder.dispatch();
        returnableOrder.finalizeOrder();
        orders.save(returnableOrder);
        assertEquals(0, orders.reserveReturnQuantity(returnableOrderId, 955L, 2, 0, 1));
        assertEquals(1, orders.reserveReturnQuantity(returnableOrderId, 955L, 2, 0, 1));
        assertThrows(IllegalArgumentException.class,
                () -> orders.reserveReturnQuantity(returnableOrderId, 955L, 2, 0, 1));
        orders.releaseReturnQuantity(returnableOrderId, 955L, 1, "rejected-return");
        orders.releaseReturnQuantity(returnableOrderId, 955L, 1, "rejected-return");
        assertEquals(1, orders.reserveReturnQuantity(returnableOrderId, 955L, 2, 0, 1));

        Long legacyOrderId = returnableOrderId + 2;
        Order legacyOrder = new Order(legacyOrderId, 952L, OrderStatus.CART);
        legacyOrder.addProduct(955L, 2, new BigDecimal("12.50"));
        legacyOrder.sendToPayment();
        legacyOrder.markAsPaid();
        legacyOrder.dispatch();
        legacyOrder.finalizeOrder();
        orders.save(legacyOrder);
        returns.save(new ReturnRequest("legacy-return-" + legacyOrderId, legacyOrderId, 952L,
                955L, 1, "Existing return", ReturnStatus.REQUESTED, Instant.now()));
        assertEquals(1, orders.reserveReturnQuantity(legacyOrderId, 955L, 2, 1, 1));
        assertThrows(IllegalArgumentException.class,
                () -> orders.reserveReturnQuantity(legacyOrderId, 955L, 2, 1, 1));

        Long concurrentReturnOrderId = returnableOrderId + 1;
        Order concurrentReturnOrder = new Order(concurrentReturnOrderId, 952L, OrderStatus.CART);
        concurrentReturnOrder.addProduct(955L, 1, new BigDecimal("12.50"));
        concurrentReturnOrder.sendToPayment();
        concurrentReturnOrder.markAsPaid();
        concurrentReturnOrder.dispatch();
        concurrentReturnOrder.finalizeOrder();
        orders.save(concurrentReturnOrder);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<Boolean> firstReservation = executor.submit(() -> tryReserveReturn(
                    concurrentReturnOrderId, ready, start));
            Future<Boolean> secondReservation = executor.submit(() -> tryReserveReturn(
                    concurrentReturnOrderId, ready, start));
            ready.await();
            start.countDown();
            assertEquals(1, (firstReservation.get() ? 1 : 0) + (secondReservation.get() ? 1 : 0));
        } finally {
            executor.shutdownNow();
        }

        Inventory returnInventory = inventories.findById(956L).orElseThrow();
        String returnOperationId = "return-retry-" + System.currentTimeMillis();
        Inventory firstReturn = returnFromSnapshot(returnInventory, 1, returnOperationId);
        assertTrue(inventories.saveIfVersionMatches(returnInventory, firstReturn));
        Inventory afterFirstReturn = inventories.findById(956L).orElseThrow();
        Inventory repeatedReturn = returnFromSnapshot(afterFirstReturn, 1, returnOperationId);
        assertTrue(inventories.saveIfVersionMatches(afterFirstReturn, repeatedReturn));
        assertEquals(afterFirstReturn.getQuantity(), inventories.findById(956L).orElseThrow().getQuantity());

        Inventory concurrentInventory = new Inventory(959L, 955L, 954L, 10);
        inventories.save(concurrentInventory);
        Inventory firstSnapshot = inventories.findById(959L).orElseThrow();
        Inventory staleSnapshot = inventories.findById(959L).orElseThrow();
        Inventory firstUpdate = reserveFromSnapshot(firstSnapshot, 7);
        Inventory staleUpdate = reserveFromSnapshot(staleSnapshot, 7);
        assertTrue(inventories.saveIfVersionMatches(firstSnapshot, firstUpdate));
        assertFalse(inventories.saveIfVersionMatches(staleSnapshot, staleUpdate));
        assertEquals(3, inventories.findById(959L).orElseThrow().getQuantity());
    }

    private static Inventory reserveFromSnapshot(Inventory inventory, int amount) {
        Inventory copy = new Inventory(inventory.getId(), inventory.getProductId(), inventory.getWarehouseId(),
                inventory.getQuantity(), inventory.getDamagedQuantity(), inventory.getMovements(),
                inventory.getVersion());
        copy.reserve(amount);
        return copy;
    }

    private static Inventory returnFromSnapshot(Inventory inventory, int amount, String operationReference) {
        Inventory copy = new Inventory(inventory.getId(), inventory.getProductId(), inventory.getWarehouseId(),
                inventory.getQuantity(), inventory.getDamagedQuantity(), inventory.getMovements(),
                inventory.getVersion());
        copy.returnInventory(amount, operationReference);
        return copy;
    }

    private boolean tryReserveReturn(Long orderId, CountDownLatch ready, CountDownLatch start)
            throws InterruptedException {
        ready.countDown();
        start.await();
        try {
            orders.reserveReturnQuantity(orderId, 955L, 1, 0, 1);
            return true;
        } catch (IllegalArgumentException alreadyReserved) {
            return false;
        }
    }
}