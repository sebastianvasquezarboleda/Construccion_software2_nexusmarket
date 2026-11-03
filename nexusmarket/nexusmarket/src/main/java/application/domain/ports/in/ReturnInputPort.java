package application.domain.ports.in;

import application.domain.models.ReturnRequest;

import java.util.List;

public interface ReturnInputPort {
    ReturnRequest request(Long orderId, Long productId, int quantity, String reason);
    ReturnRequest findById(String id);
    List<ReturnRequest> findAll();
    void approve(String id);
    void reject(String id);
    void recordRefund(String id);
}
