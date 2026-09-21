package application.domain.models;

public class Inventory {
    private final Long id;
    private final Long productId;
    private final Long warehouseId;
    private int quantity;

    public Inventory(Long id, Long productId, Long warehouseId, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }
        this.id = id;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
    }

    public void addInventory(int amount) {
        requirePositive(amount);
        quantity += amount;
    }

    public void reserve(int amount) {
        requirePositive(amount);
        if (amount > quantity) {
            throw new IllegalStateException("Insufficient inventory");
        }
        quantity -= amount;
    }

    public void removeInventory(int amount) {
        reserve(amount);
    }

    public void returnInventory(int amount) {
        addInventory(amount);
    }

    public void adjustInventory(int newQuantity) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }
        quantity = newQuantity;
    }

    public int getAvailability() {
        return quantity;
    }

    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public Long getWarehouseId() { return warehouseId; }
    public int getQuantity() { return quantity; }

    private static void requirePositive(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}