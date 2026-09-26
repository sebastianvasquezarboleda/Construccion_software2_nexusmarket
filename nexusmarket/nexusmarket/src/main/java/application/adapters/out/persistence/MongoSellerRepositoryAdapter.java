package application.adapters.out.persistence;

import application.adapters.out.persistence.document.SellerDocument;
import application.adapters.out.persistence.repository.SellerMongoRepository;
import application.domain.models.Seller;
import application.domain.ports.out.SellerRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MongoSellerRepositoryAdapter implements SellerRepositoryPort {
    private final SellerMongoRepository repository;

    public MongoSellerRepositoryAdapter(SellerMongoRepository repository) { this.repository = repository; }

    @Override
    public void save(Seller seller) {
        SellerDocument document = new SellerDocument();
        document.setId(seller.id());
        document.setUserId(seller.userId());
        repository.save(document);
    }

    @Override
    public Optional<Seller> findById(Long id) { return repository.findById(id).map(MongoSellerRepositoryAdapter::toDomain); }

    @Override
    public Optional<Seller> findByUserId(Long userId) {
        return repository.findByUserId(userId).map(MongoSellerRepositoryAdapter::toDomain);
    }

    @Override
    public List<Seller> findAll() { return repository.findAll().stream().map(MongoSellerRepositoryAdapter::toDomain).toList(); }

    private static Seller toDomain(SellerDocument document) {
        return new Seller(document.getId(), document.getUserId());
    }
}