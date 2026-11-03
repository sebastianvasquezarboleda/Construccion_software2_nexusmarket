package application.adapters.out.persistence.repository;

import application.adapters.out.persistence.entity.InvoiceDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface InvoiceMongoRepository extends MongoRepository<InvoiceDocument, String> {
    Optional<InvoiceDocument> findByOrderId(Long orderId);
}
