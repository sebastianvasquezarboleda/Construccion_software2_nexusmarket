package application.adapters.out.persistence.entity;

import application.domain.enums.ReturnStatus;
import application.domain.models.InventoryAllocation;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("returns")
public class ReturnRequestDocument {
    @Id
    private String id;
    private Long orderId;
    private Long buyerId;
    private Long productId;
    private int quantity;
    private String reason;
    private ReturnStatus status;
    private Instant createdAt;
    private List<InventoryAllocation> allocations = new ArrayList<>();

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public ReturnStatus getStatus() { return status; }
    public void setStatus(ReturnStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public List<InventoryAllocation> getAllocations() { return allocations; }
    public void setAllocations(List<InventoryAllocation> allocations) {
        this.allocations = new ArrayList<>(allocations == null ? List.of() : allocations);
    }
}
