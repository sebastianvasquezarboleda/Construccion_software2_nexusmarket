package application.adapters.out.persistence.repository;

import application.adapters.out.persistence.entity.WarehouseDocument;
import application.domain.enums.WarehouseType;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WarehouseMongoRepository extends MongoRepository<WarehouseDocument, Long> {
    Page<WarehouseDocument> findByTypeOrSellerId(WarehouseType type, Long sellerId, Pageable pageable);
}