package application.adapters.out.persistence.repository;

import application.adapters.out.persistence.document.UserDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserMongoRepository extends MongoRepository<UserDocument, Long> { }