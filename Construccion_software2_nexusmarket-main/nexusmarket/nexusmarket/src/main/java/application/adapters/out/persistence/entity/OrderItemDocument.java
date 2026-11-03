package application.adapters.out.persistence.entity;

import application.domain.models.InventoryAllocation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class OrderItemDocument {
    private Long productId;
    private int quantity;
    private BigDecimal unitPrice;
    private List<InventoryAllocation> allocations = new ArrayList<>();

    public OrderItemDocument() { }

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public List<InventoryAllocation> getAllocations() { return allocations; }
    public void setAllocations(List<InventoryAllocation> allocations) {
        this.allocations = new ArrayList<>(allocations == null ? List.of() : allocations);
    }
}