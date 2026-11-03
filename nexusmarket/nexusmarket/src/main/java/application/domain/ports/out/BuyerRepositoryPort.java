package application.domain.ports.out;

import application.domain.models.Buyer;
import java.util.Optional;

public interface BuyerRepositoryPort extends PagedRepositoryPort<Buyer> {
    void save(Buyer buyer);
    Optional<Buyer> findById(Long id);
    Optional<Buyer> findByUserId(Long userId);
}