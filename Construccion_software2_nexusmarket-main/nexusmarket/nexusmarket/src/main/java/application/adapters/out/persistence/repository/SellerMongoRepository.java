package application.adapters.out.persistence.repository;

import application.adapters.out.persistence.entity.SellerDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface SellerMongoRepository extends MongoRepository<SellerDocument, Long> {
    Optional<SellerDocument> findByUserId(Long userId);
}