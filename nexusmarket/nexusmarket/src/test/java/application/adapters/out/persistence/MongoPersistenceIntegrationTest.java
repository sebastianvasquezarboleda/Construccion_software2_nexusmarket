package application.adapters.out.persistence;

import application.domain.enums.OrderStatus;
import application.domain.enums.ProductStatus;
import application.domain.enums.ProductType;
import application.domain.enums.Role;
import application.domain.enums.UserStatus;
import application.domain.enums.WarehouseType;
import application.domain.models.Buyer;
import application.domain.models.Inventory;
import application.domain.models.Order;
import application.domain.models.Product;
import application.domain.models.Seller;
import application.domain.models.User;
import application.domain.models.Warehouse;
import application.domain.ports.out.BuyerRepositoryPort;
import application.domain.ports.out.InventoryRepositoryPort;
import application.domain.ports.out.OrderRepositoryPort;
import application.domain.ports.out.ProductRepositoryPort;
import application.domain.ports.out.SellerRepositoryPort;
import application.domain.ports.out.UserRepositoryPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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

    @Test
    void allPortsPersistAndRestoreTheirDomainObjects() {
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

        Product product = new Product(955L, "Mongo test product", "Description",
                ProductType.PHYSICAL, List.of("Blue"), ProductStatus.PUBLISHED);
        products.save(product);
        assertEquals(List.of("Blue"), products.findById(955L).orElseThrow().getVariants());

        Inventory inventory = new Inventory(956L, 955L, 954L, 8);
        inventories.save(inventory);
        assertEquals(8, inventories.findById(956L).orElseThrow().getAvailability());

        Order order = new Order(957L, 952L, OrderStatus.CART);
        order.addProduct(955L, 2, new BigDecimal("12.50"));
        order.sendToPayment();
        order.markAsPaid();
        orders.save(order);

        Order restored = orders.findById(957L).orElseThrow();
        assertEquals(OrderStatus.PAID, restored.getStatus());
        assertEquals(1, restored.getItems().size());
        assertEquals(0, new BigDecimal("25.00").compareTo(restored.getTotal()));
    }
}