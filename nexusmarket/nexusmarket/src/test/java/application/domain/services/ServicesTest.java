package application.domain.services;

import application.domain.enums.ProductStatus;
import application.domain.enums.ProductType;
import application.domain.enums.Role;
import application.domain.enums.UserStatus;
import application.domain.models.Inventory;
import application.domain.models.Product;
import application.domain.models.User;
import application.domain.services.inventory.InventoryService;
import application.domain.services.order.OrderService;
import application.domain.services.product.ProductService;
import application.domain.services.user.UserService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicesTest {

    @Test
    void userServiceRegistersAndChangesStatus() {
        UserService service = new UserService();
        service.register(new User(1L, "Ana", "ana@example.com", "123", Role.BUYER, UserStatus.BLOCKED));

        service.activate(1L);

        assertTrue(service.isActive(1L));
        assertThrows(IllegalArgumentException.class, () -> service.findById(99L));
    }

    @Test
    void productServiceDelegatesProductLifecycle() {
        ProductService service = new ProductService();
        service.register(new Product(1L, "Keyboard", "Mechanical keyboard", ProductType.PHYSICAL,
                List.of(), ProductStatus.SUSPENDED));

        service.addVariant(1L, "US layout");
        service.publish(1L);

        assertEquals(ProductStatus.PUBLISHED, service.findById(1L).getStatus());
        assertEquals(List.of("US layout"), service.findById(1L).getVariants());
    }

    @Test
    void inventoryServiceDelegatesAvailabilityRules() {
        InventoryService service = new InventoryService();
        service.register(new Inventory(1L, 10L, 20L, 5));

        service.reserve(1L, 2);

        assertEquals(3, service.getAvailability(1L));
        assertThrows(IllegalStateException.class, () -> service.reserve(1L, 4));
    }

    @Test
    void orderServiceCoordinatesOrderLifecycle() {
        OrderService service = new OrderService();
        service.create(1L, 2L);
        service.addProduct(1L, 10L, 2, new BigDecimal("12.50"));
        service.sendToPayment(1L);
        service.markAsPaid(1L);
        service.dispatch(1L);
        service.finalizeOrder(1L);

        assertEquals(new BigDecimal("25.00"), service.findById(1L).getTotal());
    }
}