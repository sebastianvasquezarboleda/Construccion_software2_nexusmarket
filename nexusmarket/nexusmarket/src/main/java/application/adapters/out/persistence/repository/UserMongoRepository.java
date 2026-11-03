package application.adapters.out.persistence.repository;

import application.adapters.out.persistence.entity.UserDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface UserMongoRepository extends MongoRepository<UserDocument, Long> {
    Optional<UserDocument> findByEmailIgnoreCase(String email);
    Optional<UserDocument> findByDocumentNumberIgnoreCase(String documentNumber);
}