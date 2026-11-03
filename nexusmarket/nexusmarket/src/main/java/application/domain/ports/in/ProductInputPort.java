package application.domain.ports.in;

import application.domain.models.Product;
import application.domain.models.PageResult;

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
    PageResult<Product> findPage(int page, int size);
    List<Product> findBySellerId(Long sellerId);
}
