package application.services.product;

import application.domain.models.Product;
import application.domain.models.PageResult;
import application.domain.ports.in.ProductInputPort;
import application.domain.ports.out.ProductRepositoryPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.function.Consumer;

@Service
public class ProductService implements ProductInputPort {
    private final ProductRepositoryPort products;

    public ProductService(ProductRepositoryPort products) {
        this.products = products;
    }

    public Product register(Product product) {
        requireProduct(product);
        requireId(product.getId());
        if (products.findById(product.getId()).isPresent()) {
            throw new IllegalArgumentException("Product already exists");
        }
        products.save(product);
        return product;
    }

    public Product findById(Long productId) {
        requireId(productId);
        return products.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("Product not found"));
    }

    public void publish(Long productId) { update(productId, Product::publish); }
    public void suspend(Long productId) { update(productId, Product::suspend); }
    public void discontinue(Long productId) { update(productId, Product::discontinue); }
    public void addVariant(Long productId, String variant) {
        requireId(productId);
        if (variant == null || variant.isBlank()) {
            throw new IllegalArgumentException("variant is required");
        }
        Product product = findById(productId);
        product.addVariant(variant);
        products.save(product);
    }

    public void updatePrice(Long productId, BigDecimal price) {
        requireId(productId);
        Product product = findById(productId);
        product.updatePrice(price);
        products.save(product);
    }

    public boolean isPhysical(Long productId) {
        requireId(productId);
        return findById(productId).isPhysical();
    }
    public boolean isDigital(Long productId) {
        requireId(productId);
        return findById(productId).isDigital();
    }
    public List<Product> findAll() { return products.findAll(); }
    public PageResult<Product> findPage(int page, int size) { return products.findPage(page, size); }
    public List<Product> findBySellerId(Long sellerId) { return products.findBySellerId(sellerId); }

    private void update(Long productId, Consumer<Product> action) {
        Product product = findById(productId);
        action.accept(product);
        products.save(product);
    }

    private static void requireProduct(Product product) {
        if (product == null) {
            throw new IllegalArgumentException("product is required");
        }
    }

    private static void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }
}