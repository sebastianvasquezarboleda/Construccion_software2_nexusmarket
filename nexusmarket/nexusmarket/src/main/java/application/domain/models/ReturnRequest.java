package application.domain.models;

import application.domain.enums.ReturnStatus;
import java.util.List;

import java.time.Instant;

public class ReturnRequest {
    private final String id;
    private final Long orderId;
    private final Long buyerId;
    private final Long productId;
    private final int quantity;
    private final String reason;
    private final Instant createdAt;
    private final List<InventoryAllocation> allocations;
    private ReturnStatus status;

    public ReturnRequest(String id, Long orderId, Long buyerId, Long productId, int quantity,
                         String reason, ReturnStatus status, Instant createdAt) {
        this(id, orderId, buyerId, productId, quantity, reason, status, createdAt, List.of());
    }

    public ReturnRequest(String id, Long orderId, Long buyerId, Long productId, int quantity,
                         String reason, ReturnStatus status, Instant createdAt,
                         List<InventoryAllocation> allocations) {
        if (id == null || id.isBlank() || orderId == null || buyerId == null || productId == null
                || quantity <= 0 || status == null || createdAt == null) {
            throw new IllegalArgumentException("Return request fields are required");
        }
        this.id = id;
        this.orderId = orderId;
        this.buyerId = buyerId;
        this.productId = productId;
        this.quantity = quantity;
        this.reason = required(reason);
        this.status = status;
        this.createdAt = createdAt;
        this.allocations = List.copyOf(allocations == null ? List.of() : allocations);
        int allocated = this.allocations.stream()
                .mapToInt(InventoryAllocation::quantity).reduce(0, Math::addExact);
        if (allocated != 0 && allocated != quantity) {
            throw new IllegalArgumentException("Return allocations must match the request quantity");
        }
    }

    public void approve() { transition(ReturnStatus.REQUESTED, ReturnStatus.APPROVED); }
    public void reject() { transition(ReturnStatus.REQUESTED, ReturnStatus.REJECTED); }
    public void recordRefund() { transition(ReturnStatus.APPROVED, ReturnStatus.REFUNDED); }

    public String getId() { return id; }
    public Long getOrderId() { return orderId; }
    public Long getBuyerId() { return buyerId; }
    public Long getProductId() { return productId; }
    public int getQuantity() { return quantity; }
    public String getReason() { return reason; }
    public ReturnStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public List<InventoryAllocation> getAllocations() { return allocations; }
    public ReturnRequest copy() {
        return new ReturnRequest(id, orderId, buyerId, productId, quantity, reason, status, createdAt, allocations);
    }

    private void transition(ReturnStatus expected, ReturnStatus next) {
        if (status != expected) throw new IllegalStateException("Invalid return transition from " + status + " to " + next);
        status = next;
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("reason is required");
        return value.trim();
    }
}
