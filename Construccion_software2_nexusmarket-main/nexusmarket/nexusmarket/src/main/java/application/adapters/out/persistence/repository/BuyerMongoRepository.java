package application.adapters.out.persistence.repository;

import application.adapters.out.persistence.entity.BuyerDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface BuyerMongoRepository extends MongoRepository<BuyerDocument, Long> {
    Optional<BuyerDocument> findByUserId(Long userId);
}