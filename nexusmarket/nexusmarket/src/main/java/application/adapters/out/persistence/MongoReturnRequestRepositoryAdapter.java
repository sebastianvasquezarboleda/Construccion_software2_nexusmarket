package application.adapters.out.persistence;

import application.adapters.out.persistence.entity.ReturnRequestDocument;
import application.adapters.out.persistence.repository.ReturnRequestMongoRepository;
import application.domain.enums.ReturnStatus;
import application.domain.models.ReturnRequest;
import application.domain.ports.out.ReturnRequestRepositoryPort;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import java.util.List;
import java.util.Optional;

@Repository
public class MongoReturnRequestRepositoryAdapter implements ReturnRequestRepositoryPort {
    private final ReturnRequestMongoRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoReturnRequestRepositoryAdapter(ReturnRequestMongoRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public ReturnRequest save(ReturnRequest request) {
        ReturnRequestDocument document = toDocument(request);
        return toDomain(repository.save(document));
    }

    @Override
    public Optional<ReturnRequest> findById(String id) { return repository.findById(id).map(MongoReturnRequestRepositoryAdapter::toDomain); }

    @Override
    public List<ReturnRequest> findByOrderId(Long orderId) {
        return repository.findByOrderId(orderId).stream().map(MongoReturnRequestRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<ReturnRequest> findAll() { return repository.findAll().stream().map(MongoReturnRequestRepositoryAdapter::toDomain).toList(); }

    @Override
    public boolean transitionStatus(String id, ReturnStatus expected, ReturnStatus next) {
        validateTransition(expected, next);
        Query query = Query.query(Criteria.where("_id").is(id).and("status").is(expected));
        return mongoTemplate.updateFirst(query, Update.update("status", next),
                ReturnRequestDocument.class).getMatchedCount() == 1;
    }

    private static ReturnRequestDocument toDocument(ReturnRequest request) {
        ReturnRequestDocument document = new ReturnRequestDocument();
        document.setId(request.getId());
        document.setOrderId(request.getOrderId());
        document.setBuyerId(request.getBuyerId());
        document.setProductId(request.getProductId());
        document.setQuantity(request.getQuantity());
        document.setReason(request.getReason());
        document.setStatus(request.getStatus());
        document.setCreatedAt(request.getCreatedAt());
        document.setAllocations(request.getAllocations());
        return document;
    }

    private static ReturnRequest toDomain(ReturnRequestDocument document) {
        return new ReturnRequest(document.getId(), document.getOrderId(), document.getBuyerId(),
                document.getProductId(), document.getQuantity(), document.getReason(),
                document.getStatus(), document.getCreatedAt(), document.getAllocations());
    }

    private static void validateTransition(ReturnStatus expected, ReturnStatus next) {
        boolean valid = expected == ReturnStatus.REQUESTED
                && (next == ReturnStatus.APPROVED || next == ReturnStatus.REJECTED)
                || expected == ReturnStatus.APPROVED && next == ReturnStatus.REFUNDED;
        if (!valid) {
            throw new IllegalArgumentException("Invalid return status transition");
        }
    }
}
