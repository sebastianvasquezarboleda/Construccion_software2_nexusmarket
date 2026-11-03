package application.adapters.out.persistence.repository;

import application.adapters.out.persistence.entity.OrderDocument;
import application.domain.enums.OrderStatus;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;

public interface OrderMongoRepository extends MongoRepository<OrderDocument, Long> {
    List<OrderDocument> findByStatusInAndUpdatedAtBefore(Collection<OrderStatus> statuses, Instant cutoff);
}