package application.domain.ports.out;

import application.domain.models.Buyer;

import java.util.List;
import java.util.Optional;

public interface BuyerRepositoryPort {
    void save(Buyer buyer);
    Optional<Buyer> findById(Long id);
    List<Buyer> findAll();
}