package application.adapters.out.persistence.repository;

import application.adapters.out.persistence.document.OrderDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface OrderMongoRepository extends MongoRepository<OrderDocument, Long> { }