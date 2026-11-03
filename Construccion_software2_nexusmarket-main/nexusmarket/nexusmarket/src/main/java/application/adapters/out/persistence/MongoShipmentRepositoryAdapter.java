package application.adapters.out.persistence;

import application.adapters.out.persistence.entity.ShipmentDocument;
import application.adapters.out.persistence.repository.ShipmentMongoRepository;
import application.domain.models.Shipment;
import application.domain.models.PageResult;
import application.domain.ports.out.ShipmentRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MongoShipmentRepositoryAdapter implements ShipmentRepositoryPort {
    private final ShipmentMongoRepository repository;

    public MongoShipmentRepositoryAdapter(ShipmentMongoRepository repository) { this.repository = repository; }

    @Override
    public Shipment save(Shipment shipment) {
        ShipmentDocument document = new ShipmentDocument();
        document.setId(shipment.getId());
        document.setOrderId(shipment.getOrderId());
        document.setDeliveryAddress(shipment.getDeliveryAddress());
        document.setStatus(shipment.getStatus());
        return toDomain(repository.save(document));
    }

    @Override
    public Optional<Shipment> findById(String id) { return repository.findById(id).map(MongoShipmentRepositoryAdapter::toDomain); }

    @Override
    public Optional<Shipment> findByOrderId(Long orderId) { return repository.findByOrderId(orderId).map(MongoShipmentRepositoryAdapter::toDomain); }

    @Override
    public List<Shipment> findAll() { return repository.findAll().stream().map(MongoShipmentRepositoryAdapter::toDomain).toList(); }

    @Override
    public PageResult<Shipment> findPage(int page, int size) {
        PageResult.validate(page, size);
        var result = repository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
        return new PageResult<>(result.getContent().stream().map(MongoShipmentRepositoryAdapter::toDomain).toList(),
                page, size, result.getTotalElements());
    }

    private static Shipment toDomain(ShipmentDocument document) {
        return new Shipment(document.getId(), document.getOrderId(), document.getDeliveryAddress(), document.getStatus());
    }
}
