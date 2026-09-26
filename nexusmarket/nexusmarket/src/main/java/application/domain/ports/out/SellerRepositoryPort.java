package application.domain.ports.out;

import application.domain.models.Seller;

import java.util.List;
import java.util.Optional;

public interface SellerRepositoryPort {
    void save(Seller seller);
    Optional<Seller> findById(Long id);
    Optional<Seller> findByUserId(Long userId);
    List<Seller> findAll();
}