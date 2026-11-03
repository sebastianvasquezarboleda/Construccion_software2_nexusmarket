package application.domain.ports.out;

import application.domain.models.ReturnRequest;
import application.domain.enums.ReturnStatus;

import java.util.List;
import java.util.Optional;

public interface ReturnRequestRepositoryPort extends PagedRepositoryPort<ReturnRequest> {
    ReturnRequest save(ReturnRequest request);
    Optional<ReturnRequest> findById(String id);
    List<ReturnRequest> findByOrderId(Long orderId);

    boolean transitionStatus(String id, ReturnStatus expected, ReturnStatus next);
}
