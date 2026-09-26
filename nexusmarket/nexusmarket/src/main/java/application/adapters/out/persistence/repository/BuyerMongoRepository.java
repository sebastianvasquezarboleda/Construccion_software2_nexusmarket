package application.adapters.out.persistence.repository;

import application.adapters.out.persistence.document.BuyerDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface BuyerMongoRepository extends MongoRepository<BuyerDocument, Long> { }