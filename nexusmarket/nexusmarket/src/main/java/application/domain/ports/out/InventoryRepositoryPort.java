package application.domain.ports.out;

import application.domain.models.Inventory;

import java.util.List;
import java.util.Optional;

public interface InventoryRepositoryPort {
    void save(Inventory inventory);
    Optional<Inventory> findById(Long id);
    List<Inventory> findAll();
}