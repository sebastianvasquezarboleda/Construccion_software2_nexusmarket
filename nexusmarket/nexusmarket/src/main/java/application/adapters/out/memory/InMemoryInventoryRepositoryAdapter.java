package application.adapters.out.memory;

import application.domain.models.Inventory;
import application.domain.ports.out.InventoryRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryInventoryRepositoryAdapter implements InventoryRepositoryPort {
    private final ConcurrentMap<Long, Inventory> inventories = new ConcurrentHashMap<>();

    @Override
    public void save(Inventory inventory) { inventories.put(inventory.getId(), inventory); }

    @Override
    public Optional<Inventory> findById(Long id) { return Optional.ofNullable(inventories.get(id)); }

    @Override
    public List<Inventory> findAll() { return List.copyOf(inventories.values()); }
}