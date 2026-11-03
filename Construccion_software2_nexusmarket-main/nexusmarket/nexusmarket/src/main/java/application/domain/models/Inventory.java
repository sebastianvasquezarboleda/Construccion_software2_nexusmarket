package application.domain.models;

import application.domain.enums.InventoryMovementType;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class Inventory {
    private final Long id;
    private final Long productId;
    private final Long warehouseId;
    private final List<InventoryMovement> movements;
    private final Long version;
    private int quantity;
    private int damagedQuantity;

    public Inventory(Long id, Long productId, Long warehouseId, int quantity) {
        this(id, productId, warehouseId, quantity, 0, initialMovements(quantity));
    }

    public Inventory(Long id, Long productId, Long warehouseId, int quantity,
                     List<InventoryMovement> movements) {
        this(id, productId, warehouseId, quantity, 0, movements);
    }

    public Inventory(Long id, Long productId, Long warehouseId, int quantity,
                     int damagedQuantity, List<InventoryMovement> movements) {
        this(id, productId, warehouseId, quantity, damagedQuantity, movements, null);
    }

    public Inventory(Long id, Long productId, Long warehouseId, int quantity,
                     int damagedQuantity, List<InventoryMovement> movements, Long version) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }
        if (warehouseId == null) {
            throw new IllegalArgumentException("warehouseId is required");
        }
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }
        if (damagedQuantity < 0) {
            throw new IllegalArgumentException("damagedQuantity cannot be negative");
        }
        this.id = id;
        this.productId = productId;
        this.warehouseId = warehouseId;
        this.quantity = quantity;
        this.damagedQuantity = damagedQuantity;
        this.movements = new ArrayList<>(movements == null ? List.of() : movements);
        if (version != null && version < 0) {
            throw new IllegalArgumentException("version cannot be negative");
        }
        this.version = version;
    }

    public void addInventory(int amount) {
        requirePositive(amount);
        quantity = Math.addExact(quantity, amount);
        recordMovement(InventoryMovementType.ENTRY, amount);
    }

    public void reserve(int amount) {
        requirePositive(amount);
        if (amount > quantity) {
            throw new IllegalStateException("Insufficient inventory");
        }
        quantity -= amount;
        recordMovement(InventoryMovementType.RESERVATION, amount);
    }

    public void removeInventory(int amount) {
        requirePositive(amount);
        if (amount > quantity) {
            throw new IllegalStateException("Insufficient inventory");
        }
        quantity -= amount;
        recordMovement(InventoryMovementType.SALE_OUT, amount);
    }

    public void confirmSale(int amount, Long orderId) {
        requirePositive(amount);
        if (orderId == null) {
            throw new IllegalArgumentException("orderId is required");
        }
        InventoryMovement existing = movements.stream()
                .filter(movement -> movement.type() == InventoryMovementType.SALE_OUT)
                .filter(movement -> orderId.equals(movement.referenceId()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            if (existing.quantity() != amount) {
                throw new IllegalStateException("Sale movement already exists with a different quantity");
            }
            return;
        }
        recordMovement(InventoryMovementType.SALE_OUT, amount, orderId);
    }

    public void returnInventory(int amount) {
        requirePositive(amount);
        quantity = Math.addExact(quantity, amount);
        recordMovement(InventoryMovementType.RETURN, amount);
    }

    public void returnInventory(int amount, String operationReference) {
        requirePositive(amount);
        if (operationReference == null || operationReference.isBlank()) {
            throw new IllegalArgumentException("operationReference is required");
        }
        InventoryMovement existing = movements.stream()
                .filter(movement -> movement.type() == InventoryMovementType.RETURN)
                .filter(movement -> operationReference.equals(movement.operationReference()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            if (existing.quantity() != amount) {
                throw new IllegalStateException("Return movement already exists with a different quantity");
            }
            return;
        }
        quantity = Math.addExact(quantity, amount);
        movements.add(new InventoryMovement(InventoryMovementType.RETURN, amount, quantity,
                Instant.now(), null, operationReference));
    }

    public void markDamaged(int amount) {
        requirePositive(amount);
        if (amount > quantity) {
            throw new IllegalStateException("Insufficient available inventory to mark as damaged");
        }
        int nextDamagedQuantity = Math.addExact(damagedQuantity, amount);
        quantity -= amount;
        damagedQuantity = nextDamagedQuantity;
        recordMovement(InventoryMovementType.DAMAGED, amount);
    }

    public void adjustInventory(int newQuantity) {
        if (newQuantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }
        int difference = Math.abs(newQuantity - quantity);
        quantity = newQuantity;
        if (difference > 0) {
            recordMovement(InventoryMovementType.ADJUSTMENT, difference);
        }
    }

    public int getAvailability() {
        return quantity;
    }

    public List<InventoryMovement> getMovements() { return List.copyOf(movements); }
    public Long getId() { return id; }
    public Long getProductId() { return productId; }
    public Long getWarehouseId() { return warehouseId; }
    public int getQuantity() { return quantity; }
    public int getDamagedQuantity() { return damagedQuantity; }
    public Long getVersion() { return version; }

    private void recordMovement(InventoryMovementType type, int amount) {
        recordMovement(type, amount, null);
    }

    private void recordMovement(InventoryMovementType type, int amount, Long referenceId) {
        movements.add(new InventoryMovement(type, amount, quantity, Instant.now(), referenceId, null));
    }

    private static List<InventoryMovement> initialMovements(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }
        if (quantity == 0) {
            return List.of();
        }
        return List.of(new InventoryMovement(InventoryMovementType.ENTRY, quantity, quantity, Instant.now()));
    }

    private static void requirePositive(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }
}