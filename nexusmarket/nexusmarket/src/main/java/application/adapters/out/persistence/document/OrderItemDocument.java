package application.adapters.out.persistence.document;

import java.math.BigDecimal;

public class OrderItemDocument {
    private Long productId;
    private int quantity;
    private BigDecimal unitPrice;

    public OrderItemDocument() { }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
}