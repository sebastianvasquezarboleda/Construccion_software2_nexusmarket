package application.services.warehouse;

import application.domain.models.Warehouse;
import application.domain.ports.out.WarehouseRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarehouseService {
    private final WarehouseRepositoryPort warehouses;

    public WarehouseService(WarehouseRepositoryPort warehouses) {
        this.warehouses = warehouses;
    }

    public Warehouse register(Warehouse warehouse) {
        requireId(warehouse.id());
        if (warehouses.findById(warehouse.id()).isPresent()) {
            throw new IllegalArgumentException("Warehouse already exists");
        }
        warehouses.save(warehouse);
        return warehouse;
    }

    public Warehouse findById(Long warehouseId) {
        return warehouses.findById(warehouseId)
                .orElseThrow(() -> new IllegalArgumentException("Warehouse not found"));
    }

    public List<Warehouse> findAll() { return warehouses.findAll(); }

    private static void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }
}