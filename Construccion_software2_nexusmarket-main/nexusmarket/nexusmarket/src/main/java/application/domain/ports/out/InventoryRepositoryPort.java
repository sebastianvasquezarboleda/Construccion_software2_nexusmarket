package application.domain.ports.out;

import application.domain.models.Inventory;
import application.domain.models.PageResult;

import java.util.Optional;
import java.util.List;

public interface InventoryRepositoryPort extends PagedRepositoryPort<Inventory> {
    void save(Inventory inventory);
    boolean saveIfVersionMatches(Inventory expected, Inventory updated);
    Optional<Inventory> findById(Long id);
    default PageResult<Inventory> findPageForProducts(List<Long> productIds, int page, int size) {
        PageResult.validate(page, size);
        if (productIds == null || productIds.isEmpty()) {
            return new PageResult<>(List.of(), page, size, 0);
        }
        return PageResult.from(findAll().stream()
                .filter(inventory -> productIds.contains(inventory.getProductId()))
                .toList(), page, size);
    }
}