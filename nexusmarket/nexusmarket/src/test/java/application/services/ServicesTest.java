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
import application.domain.models.Product;
import application.domain.models.User;
import application.services.inventory.InventoryService;
import application.services.order.OrderService;
import application.services.product.ProductService;
import application.services.user.UserService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ServicesTest {

    @Test
    void userServiceRegistersAndChangesStatus() {
        UserService service = new UserService(new InMemoryUserRepositoryAdapter());
        service.register(new User(1L, "Ana", "ana@example.com", "123", Role.BUYER, UserStatus.BLOCKED));

        service.activate(1L);

        assertTrue(service.isActive(1L));
        assertThrows(IllegalArgumentException.class, () -> service.findById(99L));
    }

    @Test
    void productServiceDelegatesProductLifecycle() {
        ProductService service = new ProductService(new InMemoryProductRepositoryAdapter());
        service.register(new Product(1L, "Keyboard", "Mechanical keyboard", ProductType.PHYSICAL,
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
    void orderServiceReservesInventoryWhenAddingProducts() {
        InventoryService inventoryService = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        inventoryService.register(new Inventory(100L, 55L, 77L, 10));
        OrderService service = new OrderService(new InMemoryOrderRepositoryAdapter(), inventoryService);

        service.create(9L, 3L);
        service.addProduct(9L, 55L, 4, new BigDecimal("7.50"));

        assertEquals(6, inventoryService.getAvailability(100L));
        assertEquals(new BigDecimal("30.00"), service.findById(9L).getTotal());
    }

    @Test
    void orderServiceCancelsOrderAndReturnsStock() {
        InventoryService inventoryService = new InventoryService(new InMemoryInventoryRepositoryAdapter());
        inventoryService.register(new Inventory(100L, 55L, 77L, 10));
        OrderService service = new OrderService(new InMemoryOrderRepositoryAdapter(), inventoryService);

        service.create(9L, 3L);
        service.addProduct(9L, 55L, 4, new BigDecimal("7.50"));
        service.cancel(9L);

        assertEquals(10, inventoryService.getAvailability(100L));
        assertTrue(service.findById(9L).getItems().isEmpty());
    }

    @Test
    void orderServiceCoordinatesOrderLifecycle() {
        OrderService service = new OrderService(new InMemoryOrderRepositoryAdapter());
        service.create(1L, 2L);
        service.addProduct(1L, 10L, 2, new BigDecimal("12.50"));
        service.sendToPayment(1L);
        service.markAsPaid(1L);
        service.dispatch(1L);
        service.finalizeOrder(1L);

        assertEquals(new BigDecimal("25.00"), service.findById(1L).getTotal());
    }
}