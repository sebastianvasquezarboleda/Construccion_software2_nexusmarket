package application.adapters.out.memory;

import application.domain.models.Product;
import application.domain.ports.out.ProductRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryProductRepositoryAdapter implements ProductRepositoryPort {
    private final ConcurrentMap<Long, Product> products = new ConcurrentHashMap<>();

    @Override
    public void save(Product product) { products.put(product.getId(), product); }

    @Override
    public Optional<Product> findById(Long id) { return Optional.ofNullable(products.get(id)); }

    @Override
    public List<Product> findAll() { return List.copyOf(products.values()); }
}