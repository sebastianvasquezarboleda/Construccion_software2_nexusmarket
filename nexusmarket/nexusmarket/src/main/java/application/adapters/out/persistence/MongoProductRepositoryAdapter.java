package application.adapters.out.persistence;

import application.adapters.out.persistence.entity.ProductDocument;
import application.adapters.out.persistence.repository.ProductMongoRepository;
import application.domain.models.Product;
import application.domain.models.PageResult;
import application.domain.ports.out.ProductRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
        document.setSellerId(product.getSellerId());
        document.setName(product.getName());
        document.setDescription(product.getDescription());
        document.setPrice(product.getPrice());
        document.setType(product.getType());
        document.setVariants(product.getVariants());
        document.setStatus(product.getStatus());
        repository.save(document);
    }

    @Override
    public Optional<Product> findById(Long id) { return repository.findById(id).map(MongoProductRepositoryAdapter::toDomain); }

    @Override
    public List<Product> findBySellerId(Long sellerId) {
        return repository.findBySellerId(sellerId).stream().map(MongoProductRepositoryAdapter::toDomain).toList();
    }

    @Override
    public List<Product> findAll() { return repository.findAll().stream().map(MongoProductRepositoryAdapter::toDomain).toList(); }

    @Override
    public PageResult<Product> findPage(int page, int size) {
        PageResult.validate(page, size);
        var result = repository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
        return new PageResult<>(result.getContent().stream().map(MongoProductRepositoryAdapter::toDomain).toList(),
                page, size, result.getTotalElements());
    }

    private static Product toDomain(ProductDocument document) {
        return new Product(document.getId(), document.getSellerId() == null ? 0L : document.getSellerId(),
                document.getName(), document.getDescription(), document.getType(),
                document.getVariants(), document.getStatus(), document.getPrice());
    }
}