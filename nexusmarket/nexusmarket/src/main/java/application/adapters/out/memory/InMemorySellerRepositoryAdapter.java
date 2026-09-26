package application.adapters.out.memory;

import application.domain.models.Seller;
import application.domain.ports.out.SellerRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemorySellerRepositoryAdapter implements SellerRepositoryPort {
    private final ConcurrentMap<Long, Seller> sellers = new ConcurrentHashMap<>();

    @Override
    public void save(Seller seller) { sellers.put(seller.id(), seller); }

    @Override
    public Optional<Seller> findById(Long id) { return Optional.ofNullable(sellers.get(id)); }

    @Override
    public Optional<Seller> findByUserId(Long userId) {
        return sellers.values().stream()
                .filter(seller -> seller.userId().equals(userId))
                .findFirst();
    }

    @Override
    public List<Seller> findAll() { return List.copyOf(sellers.values()); }
}