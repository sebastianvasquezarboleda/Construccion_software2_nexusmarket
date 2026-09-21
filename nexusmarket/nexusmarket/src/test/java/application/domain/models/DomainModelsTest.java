package application.domain.models;

import application.domain.enums.OrderStatus;
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
    void discontinuedProductCannotBePublishedAgain() {
        Product product = new Product(1L, "Keyboard", "Mechanical keyboard", ProductType.PHYSICAL,
                List.of(), ProductStatus.PUBLISHED);

        product.discontinue();

        assertThrows(IllegalStateException.class, product::publish);
    }

    @Test
    void inventoryCannotBeReservedAboveAvailability() {
        Inventory inventory = new Inventory(1L, 10L, 20L, 3);

        assertThrows(IllegalStateException.class, () -> inventory.reserve(4));
        assertEquals(3, inventory.getAvailability());
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
}