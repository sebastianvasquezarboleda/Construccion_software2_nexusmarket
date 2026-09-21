package application.domain.services.warehouse;

import application.domain.models.Warehouse;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WarehouseService {
    private final Map<Long, Warehouse> warehouses = new HashMap<>();

    public Warehouse register(Warehouse warehouse) {
        requireId(warehouse.id());
        if (warehouses.putIfAbsent(warehouse.id(), warehouse) != null) {
            throw new IllegalArgumentException("Warehouse already exists");
        }
        return warehouse;
    }

    public Warehouse findById(Long warehouseId) {
        Warehouse warehouse = warehouses.get(warehouseId);
        if (warehouse == null) {
            throw new IllegalArgumentException("Warehouse not found");
        }
        return warehouse;
    }

    public List<Warehouse> findAll() { return List.copyOf(warehouses.values()); }

    private static void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }
}