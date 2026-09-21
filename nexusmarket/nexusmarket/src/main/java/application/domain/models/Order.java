package application.domain.models;

import application.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Order {
    private final Long id;
    private final Long buyerId;
    private final List<OrderItem> items;
    private OrderStatus status;
    private BigDecimal total;

    public Order(Long id, Long buyerId, OrderStatus status) {
        this.id = id;
        this.buyerId = Objects.requireNonNull(buyerId, "buyerId is required");
        this.items = new ArrayList<>();
        this.status = Objects.requireNonNull(status, "status is required");
        this.total = BigDecimal.ZERO;
    }

    public void addProduct(Long productId, int quantity, BigDecimal unitPrice) {
        ensureCart();
        items.add(new OrderItem(productId, quantity, unitPrice));
        updateTotal();
    }

    public void removeProduct(Long productId) {
        ensureCart();
        boolean removed = items.removeIf(item -> item.productId().equals(productId));
        if (!removed) {
            throw new IllegalArgumentException("Product is not in the order");
        }
        updateTotal();
    }

    public void updateTotal() {
        total = items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public void sendToPayment() {
        if (items.isEmpty()) {
            throw new IllegalStateException("An empty order cannot be sent to payment");
        }
        transitionFrom(OrderStatus.CART, OrderStatus.PENDING_PAYMENT);
    }

    public void markAsPaid() {
        transitionFrom(OrderStatus.PENDING_PAYMENT, OrderStatus.PAID);
    }

    public void dispatch() {
        transitionFrom(OrderStatus.PAID, OrderStatus.DISPATCHED);
    }

    public void finalizeOrder() {
        transitionFrom(OrderStatus.DISPATCHED, OrderStatus.FINALIZED);
    }

    public Long getId() { return id; }
    public Long getBuyerId() { return buyerId; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotal() { return total; }

    private void ensureCart() {
        if (status != OrderStatus.CART) {
            throw new IllegalStateException("Only cart orders can be modified");
        }
    }

    private void transitionFrom(OrderStatus expected, OrderStatus next) {
        if (status != expected) {
            throw new IllegalStateException("Invalid order transition from " + status + " to " + next);
        }
        status = next;
    }
}