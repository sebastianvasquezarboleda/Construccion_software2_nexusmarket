package application.domain.ports.out;

import application.domain.models.Product;

import java.util.Optional;
import java.util.List;

public interface ProductRepositoryPort extends PagedRepositoryPort<Product> {
    void save(Product product);
    Optional<Product> findById(Long id);
    default List<Product> findBySellerId(Long sellerId) {
        return findAll().stream().filter(product -> product.getSellerId().equals(sellerId)).toList();
    }
}