package application.domain.ports.in;

import application.domain.models.Product;

import java.math.BigDecimal;
import java.util.List;

public interface ProductInputPort {
    Product register(Product product);
    Product findById(Long productId);
    void publish(Long productId);
    void suspend(Long productId);
    void discontinue(Long productId);
    void addVariant(Long productId, String variant);
    void updatePrice(Long productId, BigDecimal price);
    boolean isPhysical(Long productId);
    boolean isDigital(Long productId);
    List<Product> findAll();
}
