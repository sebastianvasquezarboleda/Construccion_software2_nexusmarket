package application.services.seller;

import application.domain.models.Seller;
import application.domain.ports.out.SellerRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SellerService {
    private final SellerRepositoryPort sellers;

    public SellerService(SellerRepositoryPort sellers) {
        this.sellers = sellers;
    }

    public Seller register(Seller seller) {
        requireId(seller.id());
        if (sellers.findById(seller.id()).isPresent()) {
            throw new IllegalArgumentException("Seller already exists");
        }
        sellers.save(seller);
        return seller;
    }

    public Seller findById(Long sellerId) {
        return sellers.findById(sellerId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found"));
    }

    public Seller findByUserId(Long userId) {
        return sellers.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("Seller not found"));
    }

    public List<Seller> findAll() { return sellers.findAll(); }

    private static void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }
}