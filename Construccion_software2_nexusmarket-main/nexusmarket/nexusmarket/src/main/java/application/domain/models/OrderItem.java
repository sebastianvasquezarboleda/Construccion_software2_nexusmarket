package application.domain.models;

import java.math.BigDecimal;
import java.util.List;

public record OrderItem(Long productId, int quantity, BigDecimal unitPrice,
                        List<InventoryAllocation> allocations) {
    public OrderItem(Long productId, int quantity, BigDecimal unitPrice) {
        this(productId, quantity, unitPrice, List.of());
    }

    public OrderItem {
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }
        if (unitPrice == null) {
            throw new IllegalArgumentException("unitPrice is required");
        }
        if (quantity <= 0) {
            throw new IllegalArgumentException("quantity must be positive");
        }
        if (unitPrice.signum() < 0) {
            throw new IllegalArgumentException("unitPrice cannot be negative");
        }
        allocations = List.copyOf(allocations == null ? List.of() : allocations);
        int allocated = allocations.stream().mapToInt(InventoryAllocation::quantity).reduce(0, Math::addExact);
        if (allocated != 0 && allocated != quantity) {
            throw new IllegalArgumentException("Inventory allocations must match the order item quantity");
        }
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}