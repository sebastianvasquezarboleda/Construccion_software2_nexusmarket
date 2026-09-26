package application.adapters.out.memory;

import application.domain.models.Buyer;
import application.domain.ports.out.BuyerRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryBuyerRepositoryAdapter implements BuyerRepositoryPort {
    private final ConcurrentMap<Long, Buyer> buyers = new ConcurrentHashMap<>();

    @Override
    public void save(Buyer buyer) { buyers.put(buyer.getId(), buyer); }

    @Override
    public Optional<Buyer> findById(Long id) { return Optional.ofNullable(buyers.get(id)); }

    @Override
    public List<Buyer> findAll() { return List.copyOf(buyers.values()); }
}