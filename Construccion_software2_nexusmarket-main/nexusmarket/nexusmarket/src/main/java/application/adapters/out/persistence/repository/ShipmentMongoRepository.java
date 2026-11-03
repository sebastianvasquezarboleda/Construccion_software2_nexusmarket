package application.adapters.out.persistence.repository;

import application.adapters.out.persistence.entity.ShipmentDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ShipmentMongoRepository extends MongoRepository<ShipmentDocument, String> {
    Optional<ShipmentDocument> findByOrderId(Long orderId);
}
