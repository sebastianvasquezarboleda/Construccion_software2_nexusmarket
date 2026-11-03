package application.adapters.out.persistence.repository;

import application.adapters.out.persistence.entity.InventoryDocument;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Collection;

public interface InventoryMongoRepository extends MongoRepository<InventoryDocument, Long> {
    Page<InventoryDocument> findByProductIdIn(Collection<Long> productIds, Pageable pageable);
}