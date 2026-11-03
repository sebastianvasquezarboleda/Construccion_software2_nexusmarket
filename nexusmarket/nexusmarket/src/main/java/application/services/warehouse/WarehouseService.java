package application.services.warehouse;

import application.domain.models.Warehouse;
import application.domain.models.PageResult;
import application.domain.ports.in.WarehouseInputPort;
import application.domain.ports.out.WarehouseRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class WarehouseService implements WarehouseInputPort {
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
    public PageResult<Warehouse> findPage(int page, int size) { return warehouses.findPage(page, size); }
    public PageResult<Warehouse> findPageForSeller(Long sellerId, int page, int size) {
        return warehouses.findPageForSeller(sellerId, page, size);
    }

    private static void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }
}