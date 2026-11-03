package application.domain.ports.out;

import application.domain.models.Warehouse;
import application.domain.models.PageResult;

import java.util.Optional;

public interface WarehouseRepositoryPort extends PagedRepositoryPort<Warehouse> {
    void save(Warehouse warehouse);
    Optional<Warehouse> findById(Long id);
    default PageResult<Warehouse> findPageForSeller(Long sellerId, int page, int size) {
        return PageResult.from(findAll().stream()
                .filter(warehouse -> warehouse.type() == application.domain.enums.WarehouseType.MARKETPLACE
                        || sellerId.equals(warehouse.sellerId()))
                .toList(), page, size);
    }
}