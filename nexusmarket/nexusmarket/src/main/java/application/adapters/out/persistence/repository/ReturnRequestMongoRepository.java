package application.adapters.out.persistence.repository;

import application.adapters.out.persistence.entity.ReturnRequestDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReturnRequestMongoRepository extends MongoRepository<ReturnRequestDocument, String> {
    List<ReturnRequestDocument> findByOrderId(Long orderId);
}
