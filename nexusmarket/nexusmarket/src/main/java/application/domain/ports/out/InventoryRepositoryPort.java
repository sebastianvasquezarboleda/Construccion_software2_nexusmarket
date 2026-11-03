package application.domain.ports.out;

import application.domain.models.Inventory;

import java.util.List;
import java.util.Optional;

public interface InventoryRepositoryPort {
    void save(Inventory inventory);
    boolean saveIfVersionMatches(Inventory expected, Inventory updated);
    Optional<Inventory> findById(Long id);
    List<Inventory> findAll();
}