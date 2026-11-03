package application.adapters.out.persistence;

import application.adapters.out.persistence.entity.WarehouseDocument;
import application.adapters.out.persistence.repository.WarehouseMongoRepository;
import application.domain.models.Warehouse;
import application.domain.models.PageResult;
import application.domain.ports.out.WarehouseRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
        document.setSellerId(warehouse.sellerId());
        repository.save(document);
    }

    @Override
    public Optional<Warehouse> findById(Long id) {
        return repository.findById(id).map(MongoWarehouseRepositoryAdapter::toDomain);
    }

    @Override
    public List<Warehouse> findAll() { return repository.findAll().stream().map(MongoWarehouseRepositoryAdapter::toDomain).toList(); }

    @Override
    public PageResult<Warehouse> findPage(int page, int size) {
        PageResult.validate(page, size);
        var result = repository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
        return new PageResult<>(result.getContent().stream().map(MongoWarehouseRepositoryAdapter::toDomain).toList(),
                page, size, result.getTotalElements());
    }

    @Override
    public PageResult<Warehouse> findPageForSeller(Long sellerId, int page, int size) {
        if (sellerId == null) {
            throw new IllegalArgumentException("sellerId is required");
        }
        PageResult.validate(page, size);
        var result = repository.findByTypeOrSellerId(application.domain.enums.WarehouseType.MARKETPLACE, sellerId,
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
        return new PageResult<>(result.getContent().stream().map(MongoWarehouseRepositoryAdapter::toDomain).toList(),
                page, size, result.getTotalElements());
    }

    private static Warehouse toDomain(WarehouseDocument document) {
        return new Warehouse(document.getId(), document.getName(), document.getType(),
                document.getSellerId());
    }
}