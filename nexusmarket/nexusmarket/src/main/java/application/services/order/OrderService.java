package application.services.order;

import application.domain.enums.OrderStatus;
import application.domain.models.Order;
import application.domain.ports.out.OrderRepositoryPort;
import application.services.inventory.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

@Service
public class OrderService {
    private final OrderRepositoryPort orders;
    private final InventoryService inventoryService;

    public OrderService(OrderRepositoryPort orders) {
        this(orders, null);
    }

    @Autowired
    public OrderService(OrderRepositoryPort orders, InventoryService inventoryService) {
        this.orders = orders;
        this.inventoryService = inventoryService;
    }

    public Order create(Long orderId, Long buyerId) {
        requireId(orderId);
        if (buyerId == null) {
            throw new IllegalArgumentException("buyerId is required");
        }
        if (orders.findById(orderId).isPresent()) {
            throw new IllegalArgumentException("Order already exists");
        }
        Order order = new Order(orderId, buyerId, OrderStatus.CART);
        orders.save(order);
        return order;
    }

    public Order findById(Long orderId) {
        requireId(orderId);
        return orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    public void addProduct(Long orderId, Long productId, int quantity, BigDecimal unitPrice) {
        requireId(orderId);
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("unitPrice is required");
        }

        if (inventoryService != null) {
            inventoryService.reserveByProduct(productId, quantity);
        }

        update(orderId, order -> order.addProduct(productId, quantity, unitPrice));
    }

    public void removeProduct(Long orderId, Long productId) {
        requireId(orderId);
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }

        Order order = findById(orderId);
        order.getItems().stream()
                .filter(item -> item.productId().equals(productId))
                .findFirst()
                .ifPresent(item -> {
                    if (inventoryService != null) {
                        inventoryService.returnForProduct(productId, item.quantity());
                    }
                });

        update(orderId, orderToUpdate -> orderToUpdate.removeProduct(productId));
    }

    public void updateTotal(Long orderId) { requireId(orderId); update(orderId, Order::updateTotal); }
    public void sendToPayment(Long orderId) { requireId(orderId); update(orderId, Order::sendToPayment); }
    public void markAsPaid(Long orderId) { requireId(orderId); update(orderId, Order::markAsPaid); }
    public void dispatch(Long orderId) { requireId(orderId); update(orderId, Order::dispatch); }
    public void finalizeOrder(Long orderId) { requireId(orderId); update(orderId, Order::finalizeOrder); }
    public void cancel(Long orderId) {
        requireId(orderId);
        Order order = findById(orderId);
        if (inventoryService != null) {
            order.getItems().forEach(item -> inventoryService.returnForProduct(item.productId(), item.quantity()));
        }
        order.cancel();
        orders.save(order);
    }
    public List<Order> findAll() { return orders.findAll(); }

    private void update(Long orderId, Consumer<Order> action) {
        Order order = findById(orderId);
        action.accept(order);
        orders.save(order);
    }

    private static void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }
}