package application.adapters.out.persistence;

import application.adapters.out.persistence.entity.InventoryDocument;
import application.adapters.out.persistence.entity.InventoryMovementDocument;
import application.adapters.out.persistence.repository.InventoryMongoRepository;
import application.domain.models.Inventory;
import application.domain.models.InventoryMovement;
import application.domain.models.PageResult;
import application.domain.ports.out.InventoryRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.Optional;

@Repository
public class MongoInventoryRepositoryAdapter implements InventoryRepositoryPort {
    private final InventoryMongoRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoInventoryRepositoryAdapter(InventoryMongoRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(Inventory inventory) {
        InventoryDocument document = new InventoryDocument();
        document.setId(inventory.getId());
        document.setProductId(inventory.getProductId());
        document.setWarehouseId(inventory.getWarehouseId());
        document.setQuantity(inventory.getQuantity());
        document.setDamagedQuantity(inventory.getDamagedQuantity());
        document.setVersion(inventory.getVersion());
        document.setMovements(inventory.getMovements().stream()
                .map(MongoInventoryRepositoryAdapter::toDocument).toList());
        repository.save(document);
    }

    @Override
    public boolean saveIfVersionMatches(Inventory expected, Inventory updated) {
        Query query = Query.query(Criteria.where("_id").is(expected.getId())
                .and("version").is(expected.getVersion()));
        Long nextVersion = expected.getVersion() == null ? 0L : Math.addExact(expected.getVersion(), 1L);
        Update update = new Update()
                .set("productId", updated.getProductId())
                .set("warehouseId", updated.getWarehouseId())
                .set("quantity", updated.getQuantity())
                .set("damagedQuantity", updated.getDamagedQuantity())
                .set("movements", updated.getMovements().stream()
                        .map(MongoInventoryRepositoryAdapter::toDocument).toList())
                .set("version", nextVersion);
        return mongoTemplate.updateFirst(query, update, InventoryDocument.class).getMatchedCount() == 1;
    }

    @Override
    public Optional<Inventory> findById(Long id) { return repository.findById(id).map(MongoInventoryRepositoryAdapter::toDomain); }

    @Override
    public List<Inventory> findAll() { return repository.findAll().stream().map(MongoInventoryRepositoryAdapter::toDomain).toList(); }

    @Override
    public PageResult<Inventory> findPage(int page, int size) {
        PageResult.validate(page, size);
        var result = repository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
        return new PageResult<>(result.getContent().stream().map(MongoInventoryRepositoryAdapter::toDomain).toList(),
                page, size, result.getTotalElements());
    }

    @Override
    public PageResult<Inventory> findPageForProducts(List<Long> productIds, int page, int size) {
        PageResult.validate(page, size);
        if (productIds == null || productIds.isEmpty()) {
            return new PageResult<>(List.of(), page, size, 0);
        }
        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        var result = repository.findByProductIdIn(productIds, pageable);
        return new PageResult<>(result.getContent().stream().map(MongoInventoryRepositoryAdapter::toDomain).toList(),
                page, size, result.getTotalElements());
    }

    private static Inventory toDomain(InventoryDocument document) {
        return new Inventory(document.getId(), document.getProductId(), document.getWarehouseId(),
                document.getQuantity(), document.getDamagedQuantity(), document.getMovements() == null ? List.of()
                : document.getMovements().stream().map(MongoInventoryRepositoryAdapter::toDomain).toList(),
                document.getVersion());
    }

    private static InventoryMovementDocument toDocument(InventoryMovement movement) {
        InventoryMovementDocument document = new InventoryMovementDocument();
        document.setType(movement.type());
        document.setQuantity(movement.quantity());
        document.setResultingQuantity(movement.resultingQuantity());
        document.setOccurredAt(movement.occurredAt());
        document.setReferenceId(movement.referenceId());
        document.setOperationReference(movement.operationReference());
        return document;
    }

    private static InventoryMovement toDomain(InventoryMovementDocument document) {
        return new InventoryMovement(document.getType(), document.getQuantity(),
                document.getResultingQuantity(), document.getOccurredAt(), document.getReferenceId(),
                document.getOperationReference());
    }
}