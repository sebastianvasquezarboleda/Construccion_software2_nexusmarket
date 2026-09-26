package application.domain.ports.out;

import application.domain.models.Product;

import java.util.List;
import java.util.Optional;

public interface ProductRepositoryPort {
    void save(Product product);
    Optional<Product> findById(Long id);
    List<Product> findAll();
}