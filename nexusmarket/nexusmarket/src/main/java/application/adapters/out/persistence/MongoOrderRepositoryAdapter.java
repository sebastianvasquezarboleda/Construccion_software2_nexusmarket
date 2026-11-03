package application.adapters.out.persistence;

import application.adapters.out.persistence.entity.OrderDocument;
import application.adapters.out.persistence.entity.OrderItemDocument;
import application.adapters.out.persistence.repository.OrderMongoRepository;
import application.domain.enums.OrderStatus;
import application.domain.models.Order;
import application.domain.models.PageResult;
import application.domain.ports.out.OrderRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.FindAndModifyOptions;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Objects;

@Repository
public class MongoOrderRepositoryAdapter implements OrderRepositoryPort {
    private final OrderMongoRepository repository;
    private final MongoTemplate mongoTemplate;

    public MongoOrderRepositoryAdapter(OrderMongoRepository repository, MongoTemplate mongoTemplate) {
        this.repository = repository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void save(Order order) {
        OrderDocument document = toDocument(order);
        if (order.getVersion() == null && !repository.existsById(order.getId())) {
            document.setVersion(0L);
            repository.insert(document);
            order.setVersion(0L);
            return;
        }

        Long expectedVersion = order.getVersion();
        Long nextVersion = expectedVersion == null ? 0L : Math.addExact(expectedVersion, 1L);
        Query query = Query.query(Criteria.where("_id").is(order.getId())
                .and("version").is(expectedVersion));
        Update update = new Update()
                .set("buyerId", document.getBuyerId())
                .set("items", document.getItems())
                .set("status", document.getStatus())
                .set("total", document.getTotal())
                .set("createdAt", document.getCreatedAt())
                .set("updatedAt", document.getUpdatedAt())
                .set("version", nextVersion);
        if (mongoTemplate.updateFirst(query, update, OrderDocument.class).getMatchedCount() != 1) {
            throw new OptimisticLockingFailureException("Order changed concurrently; reload and retry");
        }
        order.setVersion(nextVersion);
    }

    @Override
    public Optional<Order> findById(Long id) { return repository.findById(id).map(MongoOrderRepositoryAdapter::toDomain); }

    @Override
    public List<Order> findAll() { return repository.findAll().stream().map(MongoOrderRepositoryAdapter::toDomain).toList(); }

    @Override
    public PageResult<Order> findPage(int page, int size) {
        PageResult.validate(page, size);
        var result = repository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
        return new PageResult<>(result.getContent().stream().map(MongoOrderRepositoryAdapter::toDomain).toList(),
                page, size, result.getTotalElements());
    }

    @Override
    public List<Order> findExpiredUnpaid(Instant cutoff) {
        return repository.findByStatusInAndUpdatedAtBefore(
                        List.of(OrderStatus.CART, OrderStatus.PENDING_PAYMENT), cutoff)
                .stream().map(MongoOrderRepositoryAdapter::toDomain).toList();
    }

    @Override
    public int reserveReturnQuantity(Long orderId, Long productId, int purchasedQuantity,
                                     int previouslyRequestedQuantity, int quantity) {
        if (orderId == null || productId == null || quantity <= 0 || purchasedQuantity < quantity
                || previouslyRequestedQuantity < 0) {
            throw new IllegalArgumentException("Valid order, product, and return quantities are required");
        }
        String quantityField = "returnQuantities." + productId;
        Query initializeQuery = Query.query(new Criteria().andOperator(
                Criteria.where("_id").is(orderId),
                Criteria.where("status").is(application.domain.enums.OrderStatus.FINALIZED),
                Criteria.where(quantityField).exists(false)));
        OrderDocument initialized = mongoTemplate.findAndModify(initializeQuery,
                new Update().set(quantityField, Math.addExact(previouslyRequestedQuantity, quantity)),
                FindAndModifyOptions.options().returnNew(false), OrderDocument.class);
        if (initialized != null) {
            return previouslyRequestedQuantity;
        }

        Query incrementQuery = Query.query(new Criteria().andOperator(
                Criteria.where("_id").is(orderId),
                Criteria.where("status").is(application.domain.enums.OrderStatus.FINALIZED),
                Criteria.where(quantityField).lte(purchasedQuantity - quantity)));
        OrderDocument prior = mongoTemplate.findAndModify(incrementQuery,
                new Update().inc(quantityField, quantity),
                FindAndModifyOptions.options().returnNew(false), OrderDocument.class);
        if (prior == null) {
            throw new IllegalArgumentException("Return quantity exceeds the unreturned purchased quantity");
        }
        return prior.getReturnQuantities() == null
                ? 0 : prior.getReturnQuantities().getOrDefault(productId.toString(), 0);
    }

    @Override
    public void releaseReturnQuantity(Long orderId, Long productId, int quantity, String returnRequestId) {
        if (orderId == null || productId == null || quantity <= 0
                || returnRequestId == null || returnRequestId.isBlank()) {
            throw new IllegalArgumentException("Valid return reservation details are required");
        }
        String quantityField = "returnQuantities." + productId;
        Query query = Query.query(new Criteria().andOperator(
                Criteria.where("_id").is(orderId),
                Criteria.where(quantityField).gte(quantity),
                Criteria.where("releasedReturnRequestIds").ne(returnRequestId)));
        Update update = new Update().inc(quantityField, -quantity)
                .addToSet("releasedReturnRequestIds", returnRequestId);
        if (mongoTemplate.updateFirst(query, update, OrderDocument.class).getMatchedCount() != 1) {
            OrderDocument order = repository.findById(orderId)
                    .orElseThrow(() -> new IllegalArgumentException("Order not found"));
            if (order.getReleasedReturnRequestIds() == null
                    || !order.getReleasedReturnRequestIds().contains(returnRequestId)) {
                throw new IllegalStateException("Return quantity reservation cannot be released");
            }
        }
    }

    private static OrderDocument toDocument(Order order) {
        OrderDocument document = new OrderDocument();
        document.setId(order.getId());
        document.setBuyerId(order.getBuyerId());
        document.setItems(order.getItems().stream().map(item -> {
            OrderItemDocument itemDocument = new OrderItemDocument();
            itemDocument.setProductId(item.productId());
            itemDocument.setQuantity(item.quantity());
            itemDocument.setUnitPrice(item.unitPrice());
            itemDocument.setAllocations(item.allocations());
            return itemDocument;
        }).toList());
        document.setStatus(order.getStatus());
        document.setTotal(order.getTotal());
        document.setCreatedAt(order.getCreatedAt());
        document.setUpdatedAt(order.getUpdatedAt());
        document.setVersion(order.getVersion());
        return document;
    }

    private static Order toDomain(OrderDocument document) {
        Instant updatedAt = Objects.requireNonNullElseGet(document.getUpdatedAt(), Instant::now);
        Instant createdAt = Objects.requireNonNullElse(document.getCreatedAt(), updatedAt);
        Order order = new Order(document.getId(), document.getBuyerId(), OrderStatus.CART,
                createdAt, updatedAt, document.getVersion());
        for (OrderItemDocument item : document.getItems()) {
            order.addProduct(item.getProductId(), item.getQuantity(), item.getUnitPrice(), item.getAllocations());
        }
        advanceToStatus(order, document.getStatus());
        order.restoreUpdatedAt(updatedAt);
        return order;
    }

    private static void advanceToStatus(Order order, OrderStatus status) {
        if (status == OrderStatus.CART) return;
        if (status == OrderStatus.CANCELLED) {
            order.cancel();
            return;
        }
        order.sendToPayment();
        if (status == OrderStatus.PENDING_PAYMENT) return;
        order.markAsPaid();
        if (status == OrderStatus.PAID) return;
        order.dispatch();
        if (status == OrderStatus.DISPATCHED) return;
        order.finalizeOrder();
    }
}