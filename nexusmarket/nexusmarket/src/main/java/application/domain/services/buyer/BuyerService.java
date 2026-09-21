package application.domain.services.buyer;

import application.domain.models.Buyer;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class BuyerService {
    private final Map<Long, Buyer> buyers = new HashMap<>();

    public Buyer register(Buyer buyer) {
        requireId(buyer.getId());
        if (buyers.putIfAbsent(buyer.getId(), buyer) != null) {
            throw new IllegalArgumentException("Buyer already exists");
        }
        return buyer;
    }

    public Buyer findById(Long buyerId) {
        Buyer buyer = buyers.get(buyerId);
        if (buyer == null) {
            throw new IllegalArgumentException("Buyer not found");
        }
        return buyer;
    }

    public void addAddress(Long buyerId, String address) { findById(buyerId).addAddress(address); }
    public void removeAddress(Long buyerId, String address) { findById(buyerId).removeAddress(address); }
    public List<Buyer> findAll() { return List.copyOf(buyers.values()); }

    private static void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }
}