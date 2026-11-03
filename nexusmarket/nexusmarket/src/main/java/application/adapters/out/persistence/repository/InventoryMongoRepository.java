package application.adapters.out.persistence.repository;

import application.adapters.out.persistence.entity.InventoryDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface InventoryMongoRepository extends MongoRepository<InventoryDocument, Long> { }