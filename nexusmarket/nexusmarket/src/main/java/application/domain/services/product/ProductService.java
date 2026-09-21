package application.domain.services.product;

import application.domain.models.Product;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProductService {
    private final Map<Long, Product> products = new HashMap<>();

    public Product register(Product product) {
        requireId(product.getId());
        if (products.putIfAbsent(product.getId(), product) != null) {
            throw new IllegalArgumentException("Product already exists");
        }
        return product;
    }

    public Product findById(Long productId) {
        Product product = products.get(productId);
        if (product == null) {
            throw new IllegalArgumentException("Product not found");
        }
        return product;
    }

    public void publish(Long productId) { findById(productId).publish(); }
    public void suspend(Long productId) { findById(productId).suspend(); }
    public void discontinue(Long productId) { findById(productId).discontinue(); }
    public void addVariant(Long productId, String variant) { findById(productId).addVariant(variant); }
    public boolean isPhysical(Long productId) { return findById(productId).isPhysical(); }
    public boolean isDigital(Long productId) { return findById(productId).isDigital(); }
    public List<Product> findAll() { return List.copyOf(products.values()); }

    private static void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }
}