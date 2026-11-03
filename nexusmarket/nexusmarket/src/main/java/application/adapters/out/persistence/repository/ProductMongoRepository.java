package application.adapters.out.persistence.repository;

import application.adapters.out.persistence.entity.ProductDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ProductMongoRepository extends MongoRepository<ProductDocument, Long> { }