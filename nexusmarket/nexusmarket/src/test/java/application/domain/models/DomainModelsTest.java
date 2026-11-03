package application.domain.models;

import application.domain.enums.OrderStatus;
import application.domain.enums.InventoryMovementType;
import application.domain.enums.ProductStatus;
import application.domain.enums.ProductType;
import application.domain.enums.Role;
import application.domain.enums.UserStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DomainModelsTest {

    @Test
    void userCanBeActivatedAndBlocked() {
        User user = new User(1L, "Ana", "ana@example.com", "123", Role.BUYER, UserStatus.BLOCKED);

        assertFalse(user.isActive());
        user.activate();
        assertTrue(user.isActive());
        user.block();
        assertFalse(user.isActive());
    }

    @Test
    void domainEntitiesRequireTheirIdentifiersAndAssociations() {
        assertThrows(IllegalArgumentException.class,
                () -> new User(null, "Ana", "ana@example.com", "123", Role.BUYER, UserStatus.ACTIVE));
        assertThrows(IllegalArgumentException.class,
                () -> new Buyer(1L, null, "Main address", List.of(), "ACTIVE"));
        assertThrows(IllegalArgumentException.class, () -> new Warehouse(null, "Main", null));
        assertThrows(IllegalArgumentException.class,
                () -> new Product(null, "Name", "Description", ProductType.PHYSICAL, List.of(), ProductStatus.PUBLISHED));
        assertThrows(IllegalArgumentException.class, () -> new Order(null, 2L, OrderStatus.CART));
    }

    @Test
    void discontinuedProductCannotBePublishedAgain() {
        Product product = new Product(1L, 2L, "Keyboard", "Mechanical keyboard", ProductType.PHYSICAL,
                List.of(), ProductStatus.PUBLISHED);

        product.discontinue();

        assertThrows(IllegalStateException.class, product::publish);
    }

    @Test
    void productRejectsNegativePriceAndCannotChangePriceWhenDiscontinued() {
        assertThrows(IllegalArgumentException.class, () -> new Product(2L, 3L, "Item", "Description",
                ProductType.PHYSICAL, List.of(), ProductStatus.PUBLISHED, new BigDecimal("-0.01")));
        Product product = new Product(2L, 3L, "Item", "Description",
                ProductType.PHYSICAL, List.of(), ProductStatus.PUBLISHED, new BigDecimal("9.50"));
        product.discontinue();
        assertThrows(IllegalStateException.class, () -> product.updatePrice(BigDecimal.TEN));
    }

    @Test
    void inventoryCannotBeReservedAboveAvailability() {
        Inventory inventory = new Inventory(1L, 10L, 20L, 3);

        assertThrows(IllegalStateException.class, () -> inventory.reserve(4));
        assertEquals(3, inventory.getAvailability());
    }

    @Test
    void inventoryRequiresProductAndWarehouseIds() {
        assertThrows(IllegalArgumentException.class, () -> new Inventory(1L, null, 20L, 0));
        assertThrows(IllegalArgumentException.class, () -> new Inventory(1L, 10L, null, 0));
    }

    @Test
    void sellerRequiresUserAssociation() {
        assertThrows(IllegalArgumentException.class, () -> new Seller(1L, null));
    }

    @Test
    void inventoryRecordsMovementHistoryForQuantityChanges() {
        Inventory inventory = new Inventory(1L, 10L, 20L, 8);
        inventory.reserve(2);
        inventory.removeInventory(1);
        inventory.returnInventory(1);
        inventory.adjustInventory(9);

        assertEquals(List.of(
                InventoryMovementType.ENTRY,
                InventoryMovementType.RESERVATION,
                InventoryMovementType.SALE_OUT,
                InventoryMovementType.RETURN,
                InventoryMovementType.ADJUSTMENT),
                inventory.getMovements().stream().map(InventoryMovement::type).toList());
        assertEquals(List.of(8, 6, 5, 6, 9),
                inventory.getMovements().stream().map(InventoryMovement::resultingQuantity).toList());
        assertThrows(UnsupportedOperationException.class,
                () -> inventory.getMovements().clear());
    }

    @Test
    void damagedUnitsAreTrackedSeparatelyAndCannotBeReserved() {
        Inventory inventory = new Inventory(2L, 10L, 20L, 5);
        inventory.markDamaged(2);

        assertEquals(3, inventory.getAvailability());
        assertEquals(2, inventory.getDamagedQuantity());
        assertEquals(InventoryMovementType.DAMAGED,
                inventory.getMovements().get(inventory.getMovements().size() - 1).type());
        assertThrows(IllegalStateException.class, () -> inventory.reserve(4));
        assertThrows(IllegalStateException.class, () -> inventory.markDamaged(4));
    }

    @Test
    void inventoryCannotOverflowItsQuantity() {
        Inventory inventory = new Inventory(1L, 10L, 20L, Integer.MAX_VALUE);

        assertThrows(ArithmeticException.class, () -> inventory.addInventory(1));
        assertThrows(ArithmeticException.class, () -> inventory.returnInventory(1));
        assertEquals(Integer.MAX_VALUE, inventory.getQuantity());
        assertEquals(1, inventory.getMovements().size());
    }

    @Test
    void orderCalculatesTotalAndFollowsItsLifecycle() {
        Order order = new Order(1L, 2L, OrderStatus.CART);
        order.addProduct(10L, 2, new BigDecimal("12.50"));
        order.sendToPayment();
        order.markAsPaid();
        order.dispatch();
        order.finalizeOrder();

        assertEquals(new BigDecimal("25.00"), order.getTotal());
        assertEquals(OrderStatus.FINALIZED, order.getStatus());
        assertThrows(IllegalStateException.class,
                () -> order.addProduct(11L, 1, BigDecimal.ONE));
    }

    @Test
    void emptyOrderCannotBeSentToPayment() {
        Order order = new Order(1L, 2L, OrderStatus.CART);

        assertThrows(IllegalStateException.class, order::sendToPayment);
    }

    @Test
    void cancelledOrderCannotBeReused() {
        Order order = new Order(1L, 2L, OrderStatus.CART);
        order.cancel();

        assertEquals(OrderStatus.CANCELLED, order.getStatus());
        assertThrows(IllegalStateException.class,
                () -> order.addProduct(3L, 1, BigDecimal.ONE));
        assertThrows(IllegalStateException.class, order::cancel);
    }

    @Test
    void buyerValidatesRequiredFieldsAndNormalizesAddresses() {
        Buyer buyer = new Buyer(1L, 2L, "  Main street 123  ",
                List.of("  Av. Siempre Viva 742  ", "Av. Siempre Viva 742", "  Secondary address  "),
                "ACTIVE");

        assertEquals("Main street 123", buyer.getMainAddress());
        assertEquals(List.of("Av. Siempre Viva 742", "Secondary address"), buyer.getAdditionalAddresses());

        buyer.addAddress("  Secondary address  ");
        assertEquals(List.of("Av. Siempre Viva 742", "Secondary address"), buyer.getAdditionalAddresses());

        buyer.removeAddress("Secondary address");
        assertEquals(List.of("Av. Siempre Viva 742"), buyer.getAdditionalAddresses());

        assertThrows(IllegalArgumentException.class, () -> new Buyer(1L, 2L, "", null, "ACTIVE"));
        assertThrows(IllegalArgumentException.class, () -> new Buyer(1L, 2L, "Main street 123", null, " "));
    }
}