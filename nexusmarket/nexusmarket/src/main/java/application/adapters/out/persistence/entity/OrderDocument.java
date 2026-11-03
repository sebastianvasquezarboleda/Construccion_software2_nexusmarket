package application.adapters.out.persistence.entity;

import application.domain.enums.OrderStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Document("orders")
public class OrderDocument {
    @Id
    private Long id;
    private Long buyerId;
    private List<OrderItemDocument> items = new ArrayList<>();
    private OrderStatus status;
    private BigDecimal total = BigDecimal.ZERO;
    private Instant createdAt;
    private Instant updatedAt;
    private Long version;
    private Map<String, Integer> returnQuantities = new HashMap<>();
    private List<String> releasedReturnRequestIds = new ArrayList<>();

    public OrderDocument() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }
    public List<OrderItemDocument> getItems() { return items; }
    public void setItems(List<OrderItemDocument> items) { this.items = new ArrayList<>(items); }
    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public Map<String, Integer> getReturnQuantities() { return returnQuantities; }
    public void setReturnQuantities(Map<String, Integer> returnQuantities) {
        this.returnQuantities = returnQuantities == null ? new HashMap<>() : new HashMap<>(returnQuantities);
    }
    public List<String> getReleasedReturnRequestIds() { return releasedReturnRequestIds; }
    public void setReleasedReturnRequestIds(List<String> releasedReturnRequestIds) {
        this.releasedReturnRequestIds = releasedReturnRequestIds == null
                ? new ArrayList<>() : new ArrayList<>(releasedReturnRequestIds);
    }
}