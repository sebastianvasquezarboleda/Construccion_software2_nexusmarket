package application.services.inventory;

import application.domain.models.Inventory;
import application.domain.models.InventoryAllocation;
import application.domain.models.PageResult;
import application.domain.ports.in.InventoryInputPort;
import application.domain.ports.out.InventoryRepositoryPort;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.function.Consumer;

@Service
public class InventoryService implements InventoryInputPort {
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
        return inventories.findById(inventory.getId()).orElseThrow();
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
    public List<InventoryAllocation> reserveByProduct(Long productId, int amount) {
        requireId(productId);
        requireAmount(amount);
        List<Inventory> available = inventories.findAll().stream()
                .filter(item -> item.getProductId().equals(productId))
                .sorted(Comparator.comparing(Inventory::getId))
                .toList();
        long totalAvailable = available.stream().mapToLong(Inventory::getAvailability).sum();
        if (totalAvailable < amount) {
            throw new IllegalStateException("Insufficient inventory");
        }

        List<InventoryAllocation> allocations = new ArrayList<>();
        int remaining = amount;
        for (Inventory item : available) {
            int reserved = Math.min(remaining, item.getAvailability());
            if (reserved > 0) {
                allocations.add(new InventoryAllocation(item.getId(), reserved));
                remaining -= reserved;
            }
            if (remaining == 0) break;
        }
        reserveAllocations(allocations);
        return List.copyOf(allocations);
    }

    public void reserveAllocations(List<InventoryAllocation> allocations) {
        applyAllocations(allocations, true, null);
    }

    public void returnAllocations(List<InventoryAllocation> allocations) {
        applyAllocations(allocations, false, null);
    }

    @Override
    public void returnAllocations(List<InventoryAllocation> allocations, String operationReference) {
        if (operationReference == null || operationReference.isBlank()) {
            throw new IllegalArgumentException("operationReference is required");
        }
        if (allocations == null || allocations.isEmpty()) {
            throw new IllegalArgumentException("allocations are required");
        }
        java.util.Map<Long, Integer> quantitiesByInventory = new java.util.TreeMap<>();
        for (InventoryAllocation allocation : allocations) {
            quantitiesByInventory.merge(allocation.inventoryId(), allocation.quantity(), Math::addExact);
        }
        List<InventoryAllocation> aggregated = quantitiesByInventory.entrySet().stream()
                .map(entry -> new InventoryAllocation(entry.getKey(), entry.getValue()))
                .toList();
        applyAllocations(aggregated, false, operationReference);
    }

    public void confirmSaleAllocations(Long orderId, List<InventoryAllocation> allocations) {
        if (orderId == null) throw new IllegalArgumentException("orderId is required");
        if (allocations == null || allocations.isEmpty()) return;
        java.util.Map<Long, Integer> quantitiesByInventory = new java.util.TreeMap<>();
        for (InventoryAllocation allocation : allocations) {
            quantitiesByInventory.merge(allocation.inventoryId(), allocation.quantity(), Math::addExact);
        }
        quantitiesByInventory.forEach((inventoryId, quantity) ->
                update(inventoryId, item -> item.confirmSale(quantity, orderId)));
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
    public void returnForProduct(Long productId, int amount, String operationReference) {
        requireId(productId);
        requireAmount(amount);
        if (operationReference == null || operationReference.isBlank()) {
            throw new IllegalArgumentException("operationReference is required");
        }
        Inventory item = findByProductId(productId);
        update(item.getId(), inventory -> inventory.returnInventory(amount, operationReference));
    }
    public void markDamaged(Long inventoryId, int amount) {
        requireAmount(amount);
        update(inventoryId, item -> item.markDamaged(amount));
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
    public PageResult<Inventory> findPage(int page, int size) { return inventories.findPage(page, size); }
    public PageResult<Inventory> findPageForProducts(List<Long> productIds, int page, int size) {
        return inventories.findPageForProducts(productIds, page, size);
    }

    private void update(Long inventoryId, Consumer<Inventory> action) {
        Inventory current = findById(inventoryId);
        Inventory inventory = new Inventory(current.getId(), current.getProductId(), current.getWarehouseId(),
                current.getQuantity(), current.getDamagedQuantity(), current.getMovements(), current.getVersion());
        action.accept(inventory);
        if (!inventories.saveIfVersionMatches(current, inventory)) {
            throw new OptimisticLockingFailureException("Inventory changed concurrently; retry the operation");
        }
    }

    private void applyAllocations(List<InventoryAllocation> allocations, boolean reserve, String operationReference) {
        if (allocations == null || allocations.isEmpty()) {
            throw new IllegalArgumentException("allocations are required");
        }
        List<InventoryAllocation> applied = new ArrayList<>();
        try {
            for (InventoryAllocation allocation : allocations) {
                if (reserve) {
                    update(allocation.inventoryId(), item -> item.reserve(allocation.quantity()));
                } else if (operationReference != null) {
                    update(allocation.inventoryId(),
                            item -> item.returnInventory(allocation.quantity(), operationReference));
                } else {
                    update(allocation.inventoryId(), item -> item.returnInventory(allocation.quantity()));
                }
                applied.add(allocation);
            }
        } catch (RuntimeException ex) {
            if (!reserve && operationReference != null) {
                throw ex;
            }
            for (int i = applied.size() - 1; i >= 0; i--) {
                InventoryAllocation allocation = applied.get(i);
                try {
                    if (reserve) {
                        update(allocation.inventoryId(), item -> item.returnInventory(allocation.quantity()));
                    } else {
                        update(allocation.inventoryId(), item -> item.reserve(allocation.quantity()));
                    }
                } catch (RuntimeException compensationFailure) {
                    ex.addSuppressed(compensationFailure);
                }
            }
            throw ex;
        }
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