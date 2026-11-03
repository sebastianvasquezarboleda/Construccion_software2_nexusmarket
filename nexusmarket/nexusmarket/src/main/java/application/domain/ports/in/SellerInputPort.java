package application.domain.ports.in;

import application.domain.models.Seller;

import java.util.List;

public interface SellerInputPort {
    Seller register(Seller seller);
    Seller findById(Long sellerId);
    Seller findByUserId(Long userId);
    List<Seller> findAll();
}
