package application.domain.models;

public record InventoryAllocation(Long inventoryId, int quantity) {
    public InventoryAllocation {
        if (inventoryId == null || quantity <= 0) {
            throw new IllegalArgumentException("A valid inventory allocation is required");
        }
    }
}
