package application.domain.ports.in;

import application.domain.models.Warehouse;

import java.util.List;

public interface WarehouseInputPort {
    Warehouse register(Warehouse warehouse);
    Warehouse findById(Long warehouseId);
    List<Warehouse> findAll();
}
