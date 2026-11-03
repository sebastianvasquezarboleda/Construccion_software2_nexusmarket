package application.domain.ports.in;

import application.domain.models.ReturnRequest;
import application.domain.models.PageResult;

import java.util.List;

public interface ReturnInputPort {
    ReturnRequest request(Long orderId, Long productId, int quantity, String reason);
    ReturnRequest findById(String id);
    List<ReturnRequest> findAll();
    PageResult<ReturnRequest> findPage(int page, int size);
    void approve(String id);
    void reject(String id);
    void recordRefund(String id);
}
