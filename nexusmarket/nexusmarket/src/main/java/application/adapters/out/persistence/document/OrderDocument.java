package application.adapters.out.persistence.document;

import application.domain.enums.OrderStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Document("orders")
public class OrderDocument {
    @Id
    private Long id;
    private Long buyerId;
    private List<OrderItemDocument> items = new ArrayList<>();
    private OrderStatus status;
    private BigDecimal total = BigDecimal.ZERO;

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
}