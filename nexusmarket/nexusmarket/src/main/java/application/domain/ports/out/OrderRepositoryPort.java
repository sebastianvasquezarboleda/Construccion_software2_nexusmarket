package application.domain.ports.out;

import application.domain.models.Order;

import java.util.List;
import java.util.Optional;

public interface OrderRepositoryPort {
    void save(Order order);
    Optional<Order> findById(Long id);
    List<Order> findAll();
}