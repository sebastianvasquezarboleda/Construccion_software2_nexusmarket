package application.adapters.out.persistence;

import application.adapters.out.persistence.document.OrderDocument;
import application.adapters.out.persistence.document.OrderItemDocument;
import application.adapters.out.persistence.repository.OrderMongoRepository;
import application.domain.enums.OrderStatus;
import application.domain.models.Order;
import application.domain.ports.out.OrderRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MongoOrderRepositoryAdapter implements OrderRepositoryPort {
    private final OrderMongoRepository repository;

    public MongoOrderRepositoryAdapter(OrderMongoRepository repository) { this.repository = repository; }

    @Override
    public void save(Order order) {
        OrderDocument document = new OrderDocument();
        document.setId(order.getId());
        document.setBuyerId(order.getBuyerId());
        document.setItems(order.getItems().stream().map(item -> {
            OrderItemDocument itemDocument = new OrderItemDocument();
            itemDocument.setProductId(item.productId());
            itemDocument.setQuantity(item.quantity());
            itemDocument.setUnitPrice(item.unitPrice());
            return itemDocument;
        }).toList());
        document.setStatus(order.getStatus());
        document.setTotal(order.getTotal());
        repository.save(document);
    }

    @Override
    public Optional<Order> findById(Long id) { return repository.findById(id).map(MongoOrderRepositoryAdapter::toDomain); }

    @Override
    public List<Order> findAll() { return repository.findAll().stream().map(MongoOrderRepositoryAdapter::toDomain).toList(); }

    private static Order toDomain(OrderDocument document) {
        Order order = new Order(document.getId(), document.getBuyerId(), OrderStatus.CART);
        for (OrderItemDocument item : document.getItems()) {
            order.addProduct(item.getProductId(), item.getQuantity(), item.getUnitPrice());
        }
        advanceToStatus(order, document.getStatus());
        return order;
    }

    private static void advanceToStatus(Order order, OrderStatus status) {
        if (status == OrderStatus.CART) return;
        order.sendToPayment();
        if (status == OrderStatus.PENDING_PAYMENT) return;
        order.markAsPaid();
        if (status == OrderStatus.PAID) return;
        order.dispatch();
        if (status == OrderStatus.DISPATCHED) return;
        order.finalizeOrder();
    }
}