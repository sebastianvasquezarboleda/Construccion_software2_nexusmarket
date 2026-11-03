package application.domain.ports.in;

import application.domain.models.Order;
import application.domain.models.PageResult;

import java.time.Instant;
import java.util.List;

public interface OrderInputPort {
    Order create(Long orderId, Long buyerId);
    Order findById(Long orderId);
    void addProduct(Long orderId, Long productId, int quantity);
    void removeProduct(Long orderId, Long productId);
    void updateTotal(Long orderId);
    void sendToPayment(Long orderId);
    void markAsPaid(Long orderId);
    void dispatch(Long orderId);
    void finalizeOrder(Long orderId);
    void cancel(Long orderId);
    int expireAbandonedOrders(Instant cutoff);
    int reserveReturnQuantity(Long orderId, Long productId, int purchasedQuantity,
                              int previouslyRequestedQuantity, int quantity);
    void releaseReturnQuantity(Long orderId, Long productId, int quantity, String returnRequestId);
    List<Order> findAll();
    PageResult<Order> findPage(int page, int size);
}
