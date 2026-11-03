package application.domain.ports.out;

import application.domain.models.ReturnRequest;
import application.domain.enums.ReturnStatus;

import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepositoryPort {
    ReturnRequest save(ReturnRequest request);
    Optional<ReturnRequest> findById(String id);
    List<ReturnRequest> findByOrderId(Long orderId);
    List<ReturnRequest> findAll();
    boolean transitionStatus(String id, ReturnStatus expected, ReturnStatus next);
}
