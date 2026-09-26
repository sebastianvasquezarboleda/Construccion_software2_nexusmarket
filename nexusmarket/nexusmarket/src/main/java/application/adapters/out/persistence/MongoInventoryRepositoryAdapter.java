package application.adapters.out.persistence;

import application.adapters.out.persistence.document.InventoryDocument;
import application.adapters.out.persistence.repository.InventoryMongoRepository;
import application.domain.models.Inventory;
import application.domain.ports.out.InventoryRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MongoInventoryRepositoryAdapter implements InventoryRepositoryPort {
    private final InventoryMongoRepository repository;

    public MongoInventoryRepositoryAdapter(InventoryMongoRepository repository) { this.repository = repository; }

    @Override
    public void save(Inventory inventory) {
        InventoryDocument document = new InventoryDocument();
        document.setId(inventory.getId());
        document.setProductId(inventory.getProductId());
        document.setWarehouseId(inventory.getWarehouseId());
        document.setQuantity(inventory.getQuantity());
        repository.save(document);
    }

    @Override
    public Optional<Inventory> findById(Long id) { return repository.findById(id).map(MongoInventoryRepositoryAdapter::toDomain); }

    @Override
    public List<Inventory> findAll() { return repository.findAll().stream().map(MongoInventoryRepositoryAdapter::toDomain).toList(); }

    private static Inventory toDomain(InventoryDocument document) {
        return new Inventory(document.getId(), document.getProductId(), document.getWarehouseId(), document.getQuantity());
    }
}