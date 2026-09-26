package application.adapters.out.persistence.repository;

import application.adapters.out.persistence.document.WarehouseDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WarehouseMongoRepository extends MongoRepository<WarehouseDocument, Long> { }