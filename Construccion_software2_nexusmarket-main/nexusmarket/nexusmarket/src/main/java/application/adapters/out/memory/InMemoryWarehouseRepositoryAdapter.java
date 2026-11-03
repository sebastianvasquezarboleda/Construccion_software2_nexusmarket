package application.adapters.out.memory;

import application.domain.models.Warehouse;
import application.domain.ports.out.WarehouseRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryWarehouseRepositoryAdapter implements WarehouseRepositoryPort {
    private final ConcurrentMap<Long, Warehouse> warehouses = new ConcurrentHashMap<>();

    @Override
    public void save(Warehouse warehouse) { warehouses.put(warehouse.id(), warehouse); }

    @Override
    public Optional<Warehouse> findById(Long id) { return Optional.ofNullable(warehouses.get(id)); }

    @Override
    public List<Warehouse> findAll() { return List.copyOf(warehouses.values()); }
}