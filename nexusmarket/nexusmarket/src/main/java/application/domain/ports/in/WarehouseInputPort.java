package application.domain.ports.in;

import application.domain.models.Warehouse;
import application.domain.models.PageResult;

import java.util.List;

public interface WarehouseInputPort {
    Warehouse register(Warehouse warehouse);
    Warehouse findById(Long warehouseId);
    List<Warehouse> findAll();
    PageResult<Warehouse> findPage(int page, int size);
    PageResult<Warehouse> findPageForSeller(Long sellerId, int page, int size);
}
