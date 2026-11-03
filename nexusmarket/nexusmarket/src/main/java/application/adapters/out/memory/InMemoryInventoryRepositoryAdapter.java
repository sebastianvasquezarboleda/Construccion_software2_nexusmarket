package application.adapters.out.memory;

import application.domain.models.Inventory;
import application.domain.ports.out.InventoryRepositoryPort;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryInventoryRepositoryAdapter implements InventoryRepositoryPort {
    private final ConcurrentMap<Long, Inventory> inventories = new ConcurrentHashMap<>();

    @Override
    public void save(Inventory inventory) {
        Long version = inventory.getVersion() == null ? 0L : inventory.getVersion();
        inventories.put(inventory.getId(), copyWithVersion(inventory, version));
    }

    @Override
    public synchronized boolean saveIfVersionMatches(Inventory expected, Inventory updated) {
        Inventory current = inventories.get(expected.getId());
        if (current == null || !sameVersionAndQuantity(current, expected)) {
            return false;
        }
        Long nextVersion = current.getVersion() == null ? 0L : Math.addExact(current.getVersion(), 1L);
        inventories.put(updated.getId(), copyWithVersion(updated, nextVersion));
        return true;
    }

    @Override
    public Optional<Inventory> findById(Long id) { return Optional.ofNullable(inventories.get(id)); }

    @Override
    public List<Inventory> findAll() { return List.copyOf(inventories.values()); }

    private static boolean sameVersionAndQuantity(Inventory current, Inventory expected) {
        return java.util.Objects.equals(current.getVersion(), expected.getVersion())
                && current.getQuantity() == expected.getQuantity()
                && current.getDamagedQuantity() == expected.getDamagedQuantity()
                && current.getMovements().equals(expected.getMovements());
    }

    private static Inventory copyWithVersion(Inventory inventory, Long version) {
        return new Inventory(inventory.getId(), inventory.getProductId(), inventory.getWarehouseId(),
                inventory.getQuantity(), inventory.getDamagedQuantity(), inventory.getMovements(), version);
    }
}