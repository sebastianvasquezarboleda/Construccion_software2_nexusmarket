package application.adapters.out.memory;

import application.domain.models.Order;
import application.domain.ports.out.OrderRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryOrderRepositoryAdapter implements OrderRepositoryPort {
    private final ConcurrentMap<Long, Order> orders = new ConcurrentHashMap<>();

    @Override
    public void save(Order order) { orders.put(order.getId(), order); }

    @Override
    public Optional<Order> findById(Long id) { return Optional.ofNullable(orders.get(id)); }

    @Override
    public List<Order> findAll() { return List.copyOf(orders.values()); }
}