package application.domain.services.order;

import application.domain.enums.OrderStatus;
import application.domain.models.Order;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OrderService {
    private final Map<Long, Order> orders = new HashMap<>();

    public Order create(Long orderId, Long buyerId) {
        requireId(orderId);
        Order order = new Order(orderId, buyerId, OrderStatus.CART);
        if (orders.putIfAbsent(orderId, order) != null) {
            throw new IllegalArgumentException("Order already exists");
        }
        return order;
    }

    public Order findById(Long orderId) {
        Order order = orders.get(orderId);
        if (order == null) {
            throw new IllegalArgumentException("Order not found");
        }
        return order;
    }

    public void addProduct(Long orderId, Long productId, int quantity, BigDecimal unitPrice) {
        findById(orderId).addProduct(productId, quantity, unitPrice);
    }

    public void removeProduct(Long orderId, Long productId) { findById(orderId).removeProduct(productId); }
    public void updateTotal(Long orderId) { findById(orderId).updateTotal(); }
    public void sendToPayment(Long orderId) { findById(orderId).sendToPayment(); }
    public void markAsPaid(Long orderId) { findById(orderId).markAsPaid(); }
    public void dispatch(Long orderId) { findById(orderId).dispatch(); }
    public void finalizeOrder(Long orderId) { findById(orderId).finalizeOrder(); }
    public List<Order> findAll() { return List.copyOf(orders.values()); }

    private static void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }
}