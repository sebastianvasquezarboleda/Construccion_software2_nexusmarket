package application.adapters.out.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Document("invoices")
public class InvoiceDocument {
    @Id
    private String id;
    @Indexed(unique = true, name = "invoices_order_unique")
    private Long orderId;
    private Long buyerId;
    private List<InvoiceLineDocument> items = new ArrayList<>();
    private BigDecimal total;
    private Instant issuedAt;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getBuyerId() { return buyerId; }
    public void setBuyerId(Long buyerId) { this.buyerId = buyerId; }
    public List<InvoiceLineDocument> getItems() { return items; }
    public void setItems(List<InvoiceLineDocument> items) { this.items = items == null ? new ArrayList<>() : new ArrayList<>(items); }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
    public Instant getIssuedAt() { return issuedAt; }
    public void setIssuedAt(Instant issuedAt) { this.issuedAt = issuedAt; }
}
