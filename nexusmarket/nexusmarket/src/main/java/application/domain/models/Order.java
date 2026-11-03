package application.domain.models;

import application.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Order {
    private final Long id;
    private final Long buyerId;
    private final List<OrderItem> items;
    private final Instant createdAt;
    private OrderStatus status;
    private BigDecimal total;
    private Instant updatedAt;
    private Long version;

    public Order(Long id, Long buyerId, OrderStatus status) {
        this(id, buyerId, status, Instant.now(), Instant.now(), null);
    }

    public Order(Long id, Long buyerId, OrderStatus status, Instant createdAt,
                 Instant updatedAt, Long version) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (buyerId == null) {
            throw new IllegalArgumentException("buyerId is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (createdAt == null || updatedAt == null || updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("valid order timestamps are required");
        }
        if (version != null && version < 0) {
            throw new IllegalArgumentException("version cannot be negative");
        }
        this.id = id;
        this.buyerId = buyerId;
        this.items = new ArrayList<>();
        this.createdAt = createdAt;
        this.status = status;
        this.total = BigDecimal.ZERO;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    private Order(Order source) {
        this.id = source.id;
        this.buyerId = source.buyerId;
        this.items = new ArrayList<>(source.items);
        this.createdAt = source.createdAt;
        this.status = source.status;
        this.total = source.total;
        this.updatedAt = source.updatedAt;
        this.version = source.version;
    }

    public Order copy() {
        return new Order(this);
    }

    public void addProduct(Long productId, int quantity, BigDecimal unitPrice) {
        addProduct(productId, quantity, unitPrice, List.of());
    }

    public void addProduct(Long productId, int quantity, BigDecimal unitPrice,
                           List<InventoryAllocation> allocations) {
        validateCanAddProduct(productId, quantity, unitPrice);
        items.add(new OrderItem(productId, quantity, unitPrice, allocations));
        updateTotal();
        touch();
    }

    public void removeProduct(Long productId) {
        validateCanRemoveProduct(productId);
        items.removeIf(item -> item.productId().equals(productId));
        updateTotal();
        touch();
    }

    public void validateCanAddProduct(Long productId, int quantity, BigDecimal unitPrice) {
        ensureCart();
        new OrderItem(productId, quantity, unitPrice);
    }

    public int validateCanRemoveProduct(Long productId) {
        ensureCart();
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }
        int quantity = items.stream()
                .filter(item -> item.productId().equals(productId))
                .mapToInt(OrderItem::quantity)
                .reduce(0, Math::addExact);
        if (quantity == 0) {
            throw new IllegalArgumentException("Product is not in the order");
        }
        return quantity;
    }

    public void updateTotal() {
        total = items.stream()
                .map(OrderItem::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        touch();
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

    public void cancel() {
        validateCanCancel();
        items.clear();
        total = BigDecimal.ZERO;
        status = OrderStatus.CANCELLED;
        touch();
    }

    public void validateCanCancel() {
        if (status != OrderStatus.CART && status != OrderStatus.PENDING_PAYMENT) {
            throw new IllegalStateException("Only unpaid orders can be cancelled");
        }
    }

    public Long getId() { return id; }
    public Long getBuyerId() { return buyerId; }
    public List<OrderItem> getItems() { return List.copyOf(items); }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void restoreUpdatedAt(Instant updatedAt) {
        if (updatedAt == null || updatedAt.isBefore(createdAt)) {
            throw new IllegalArgumentException("valid updatedAt is required");
        }
        this.updatedAt = updatedAt;
    }
    public Long getVersion() { return version; }
    public void setVersion(Long version) {
        if (version == null || version < 0) {
            throw new IllegalArgumentException("version must be non-negative");
        }
        this.version = version;
    }

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
        touch();
    }

    private void touch() {
        updatedAt = Instant.now();
    }
}