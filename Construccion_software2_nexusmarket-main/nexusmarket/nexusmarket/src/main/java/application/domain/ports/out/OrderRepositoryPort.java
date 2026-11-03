package application.domain.ports.out;

import application.domain.models.Order;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepositoryPort extends PagedRepositoryPort<Order> {
    void save(Order order);
    Optional<Order> findById(Long id);
    List<Order> findExpiredUnpaid(Instant cutoff);
    int reserveReturnQuantity(Long orderId, Long productId, int purchasedQuantity,
                              int previouslyRequestedQuantity, int quantity);
    void releaseReturnQuantity(Long orderId, Long productId, int quantity, String returnRequestId);
}