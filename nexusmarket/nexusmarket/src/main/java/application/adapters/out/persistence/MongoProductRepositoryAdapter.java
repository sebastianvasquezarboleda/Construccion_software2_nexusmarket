package application.adapters.out.persistence;

import application.adapters.out.persistence.document.ProductDocument;
import application.adapters.out.persistence.repository.ProductMongoRepository;
import application.domain.models.Product;
import application.domain.ports.out.ProductRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MongoProductRepositoryAdapter implements ProductRepositoryPort {
    private final ProductMongoRepository repository;

    public MongoProductRepositoryAdapter(ProductMongoRepository repository) { this.repository = repository; }

    @Override
    public void save(Product product) {
        ProductDocument document = new ProductDocument();
        document.setId(product.getId());
        document.setName(product.getName());
        document.setDescription(product.getDescription());
        document.setType(product.getType());
        document.setVariants(product.getVariants());
        document.setStatus(product.getStatus());
        repository.save(document);
    }

    @Override
    public Optional<Product> findById(Long id) { return repository.findById(id).map(MongoProductRepositoryAdapter::toDomain); }

    @Override
    public List<Product> findAll() { return repository.findAll().stream().map(MongoProductRepositoryAdapter::toDomain).toList(); }

    private static Product toDomain(ProductDocument document) {
        return new Product(document.getId(), document.getName(), document.getDescription(), document.getType(),
                document.getVariants(), document.getStatus());
    }
}