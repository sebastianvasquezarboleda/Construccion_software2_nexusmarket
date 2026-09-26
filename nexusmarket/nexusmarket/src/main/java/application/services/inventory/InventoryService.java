package application.services.inventory;

import application.domain.models.Inventory;
import application.domain.ports.out.InventoryRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.function.Consumer;

@Service
public class InventoryService {
    private final InventoryRepositoryPort inventories;

    public InventoryService(InventoryRepositoryPort inventories) {
        this.inventories = inventories;
    }

    public Inventory register(Inventory inventory) {
        requireInventory(inventory);
        requireId(inventory.getId());
        if (inventories.findById(inventory.getId()).isPresent()) {
            throw new IllegalArgumentException("Inventory already exists");
        }
        inventories.save(inventory);
        return inventory;
    }

    public Inventory findById(Long inventoryId) {
        requireId(inventoryId);
        return inventories.findById(inventoryId)
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found"));
    }

    public Inventory findByProductId(Long productId) {
        requireId(productId);
        return inventories.findAll().stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Inventory not found for product " + productId));
    }

    public void add(Long inventoryId, int amount) { requireAmount(amount); update(inventoryId, item -> item.addInventory(amount)); }
    public void reserve(Long inventoryId, int amount) { requireAmount(amount); update(inventoryId, item -> item.reserve(amount)); }
    public void reserveByProduct(Long productId, int amount) {
        requireId(productId);
        requireAmount(amount);
        Inventory item = findByProductId(productId);
        reserve(item.getId(), amount);
    }
    public void remove(Long inventoryId, int amount) { requireAmount(amount); update(inventoryId, item -> item.removeInventory(amount)); }
    public void returnInventory(Long inventoryId, int amount) {
        requireAmount(amount);
        update(inventoryId, item -> item.returnInventory(amount));
    }
    public void returnForProduct(Long productId, int amount) {
        requireId(productId);
        requireAmount(amount);
        Inventory item = findByProductId(productId);
        returnInventory(item.getId(), amount);
    }
    public void adjust(Long inventoryId, int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }
        update(inventoryId, item -> item.adjustInventory(quantity));
    }

    public int getAvailability(Long inventoryId) {
        requireId(inventoryId);
        return findById(inventoryId).getAvailability();
    }
    public List<Inventory> findAll() { return inventories.findAll(); }

    private void update(Long inventoryId, Consumer<Inventory> action) {
        Inventory inventory = findById(inventoryId);
        action.accept(inventory);
        inventories.save(inventory);
    }

    private static void requireInventory(Inventory inventory) {
        if (inventory == null) {
            throw new IllegalArgumentException("inventory is required");
        }
    }

    private static void requireAmount(int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("amount must be positive");
        }
    }

    private static void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }
}