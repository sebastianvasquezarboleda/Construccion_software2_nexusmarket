package application.domain.services.inventory;

import application.domain.models.Inventory;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class InventoryService {
    private final Map<Long, Inventory> inventories = new HashMap<>();

    public Inventory register(Inventory inventory) {
        requireId(inventory.getId());
        if (inventories.putIfAbsent(inventory.getId(), inventory) != null) {
            throw new IllegalArgumentException("Inventory already exists");
        }
        return inventory;
    }

    public Inventory findById(Long inventoryId) {
        Inventory inventory = inventories.get(inventoryId);
        if (inventory == null) {
            throw new IllegalArgumentException("Inventory not found");
        }
        return inventory;
    }

    public void add(Long inventoryId, int amount) { findById(inventoryId).addInventory(amount); }
    public void reserve(Long inventoryId, int amount) { findById(inventoryId).reserve(amount); }
    public void remove(Long inventoryId, int amount) { findById(inventoryId).removeInventory(amount); }
    public void returnInventory(Long inventoryId, int amount) { findById(inventoryId).returnInventory(amount); }
    public void adjust(Long inventoryId, int quantity) { findById(inventoryId).adjustInventory(quantity); }
    public int getAvailability(Long inventoryId) { return findById(inventoryId).getAvailability(); }
    public List<Inventory> findAll() { return List.copyOf(inventories.values()); }

    private static void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }
}