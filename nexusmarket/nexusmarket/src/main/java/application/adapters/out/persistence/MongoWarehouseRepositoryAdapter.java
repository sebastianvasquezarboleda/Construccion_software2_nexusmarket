package application.adapters.out.persistence;

import application.adapters.out.persistence.document.WarehouseDocument;
import application.adapters.out.persistence.repository.WarehouseMongoRepository;
import application.domain.models.Warehouse;
import application.domain.ports.out.WarehouseRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MongoWarehouseRepositoryAdapter implements WarehouseRepositoryPort {
    private final WarehouseMongoRepository repository;

    public MongoWarehouseRepositoryAdapter(WarehouseMongoRepository repository) { this.repository = repository; }

    @Override
    public void save(Warehouse warehouse) {
        WarehouseDocument document = new WarehouseDocument();
        document.setId(warehouse.id());
        document.setName(warehouse.name());
        document.setType(warehouse.type());
        repository.save(document);
    }

    @Override
    public Optional<Warehouse> findById(Long id) {
        return repository.findById(id).map(MongoWarehouseRepositoryAdapter::toDomain);
    }

    @Override
    public List<Warehouse> findAll() { return repository.findAll().stream().map(MongoWarehouseRepositoryAdapter::toDomain).toList(); }

    private static Warehouse toDomain(WarehouseDocument document) {
        return new Warehouse(document.getId(), document.getName(), document.getType());
    }
}