package application.adapters.out.memory;

import application.domain.models.Order;
import application.domain.ports.out.OrderRepositoryPort;
import org.springframework.dao.OptimisticLockingFailureException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.HashSet;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryOrderRepositoryAdapter implements OrderRepositoryPort {
    private final ConcurrentMap<Long, Order> orders = new ConcurrentHashMap<>();
    private final Map<Long, Map<Long, Integer>> returnQuantities = new HashMap<>();
    private final Set<String> releasedReturnRequests = new HashSet<>();

    @Override
    public synchronized void save(Order order) {
        Order current = orders.get(order.getId());
        if (current == null) {
            if (order.getVersion() != null) {
                throw new OptimisticLockingFailureException("Order does not exist at the expected version");
            }
            order.setVersion(0L);
        } else {
            if (!java.util.Objects.equals(current.getVersion(), order.getVersion())) {
                throw new OptimisticLockingFailureException("Order changed concurrently; reload and retry");
            }
            order.setVersion(Math.addExact(current.getVersion(), 1L));
        }
        orders.put(order.getId(), order);
    }

    @Override
    public Optional<Order> findById(Long id) { return Optional.ofNullable(orders.get(id)); }

    @Override
    public List<Order> findAll() { return List.copyOf(orders.values()); }

    @Override
    public List<Order> findExpiredUnpaid(Instant cutoff) {
        return orders.values().stream()
                .filter(order -> order.getStatus() == application.domain.enums.OrderStatus.CART
                        || order.getStatus() == application.domain.enums.OrderStatus.PENDING_PAYMENT)
                .filter(order -> order.getUpdatedAt().isBefore(cutoff))
                .toList();
    }

    @Override
    public synchronized int reserveReturnQuantity(Long orderId, Long productId,
                                                  int purchasedQuantity, int previouslyRequestedQuantity,
                                                  int quantity) {
        if (quantity <= 0 || previouslyRequestedQuantity < 0 || purchasedQuantity < quantity) {
            throw new IllegalArgumentException("Return quantity exceeds the purchased quantity");
        }
        Order order = orders.get(orderId);
        if (order == null || order.getStatus() != application.domain.enums.OrderStatus.FINALIZED) {
            throw new IllegalStateException("Returns can only be requested for finalized orders");
        }
        Map<Long, Integer> byProduct = returnQuantities.computeIfAbsent(orderId, ignored -> new HashMap<>());
        int reserved = byProduct.getOrDefault(productId, previouslyRequestedQuantity);
        if (quantity > purchasedQuantity - reserved) {
            throw new IllegalArgumentException("Return quantity exceeds the unreturned purchased quantity");
        }
        byProduct.put(productId, Math.addExact(reserved, quantity));
        return reserved;
    }

    @Override
    public synchronized void releaseReturnQuantity(Long orderId, Long productId, int quantity,
                                                   String returnRequestId) {
        String releaseKey = orderId + ":" + returnRequestId;
        if (releasedReturnRequests.contains(releaseKey)) return;
        Map<Long, Integer> byProduct = returnQuantities.get(orderId);
        int reserved = byProduct == null ? 0 : byProduct.getOrDefault(productId, 0);
        if (quantity <= 0 || reserved < quantity) {
            throw new IllegalStateException("Return quantity reservation cannot be released");
        }
        byProduct.put(productId, reserved - quantity);
        releasedReturnRequests.add(releaseKey);
    }
}