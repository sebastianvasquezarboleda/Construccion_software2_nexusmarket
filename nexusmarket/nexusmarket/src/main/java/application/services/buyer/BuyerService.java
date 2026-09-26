package application.services.buyer;

import application.domain.models.Buyer;
import application.domain.ports.out.BuyerRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BuyerService {
    private final BuyerRepositoryPort buyers;

    public BuyerService(BuyerRepositoryPort buyers) {
        this.buyers = buyers;
    }

    public Buyer register(Buyer buyer) {
        requireId(buyer.getId());
        if (buyers.findById(buyer.getId()).isPresent()) {
            throw new IllegalArgumentException("Buyer already exists");
        }
        buyers.save(buyer);
        return buyer;
    }

    public Buyer findById(Long buyerId) {
        return buyers.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("Buyer not found"));
    }

    public void addAddress(Long buyerId, String address) {
        Buyer buyer = findById(buyerId);
        buyer.addAddress(address);
        buyers.save(buyer);
    }

    public void removeAddress(Long buyerId, String address) {
        Buyer buyer = findById(buyerId);
        buyer.removeAddress(address);
        buyers.save(buyer);
    }

    public List<Buyer> findAll() { return buyers.findAll(); }

    private static void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }
}