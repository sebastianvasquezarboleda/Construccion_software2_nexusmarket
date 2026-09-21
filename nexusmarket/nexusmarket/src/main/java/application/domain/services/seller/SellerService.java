package application.domain.services.seller;

import application.domain.models.Seller;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SellerService {
    private final Map<Long, Seller> sellers = new HashMap<>();

    public Seller register(Seller seller) {
        requireId(seller.id());
        if (sellers.putIfAbsent(seller.id(), seller) != null) {
            throw new IllegalArgumentException("Seller already exists");
        }
        return seller;
    }

    public Seller findById(Long sellerId) {
        Seller seller = sellers.get(sellerId);
        if (seller == null) {
            throw new IllegalArgumentException("Seller not found");
        }
        return seller;
    }

    public Seller findByUserId(Long userId) {
        return sellers.values().stream()
                .filter(seller -> seller.userId().equals(userId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Seller not found"));
    }

    public List<Seller> findAll() { return List.copyOf(sellers.values()); }

    private static void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }
}