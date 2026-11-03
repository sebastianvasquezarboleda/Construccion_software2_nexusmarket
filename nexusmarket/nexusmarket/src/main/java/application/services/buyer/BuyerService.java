package application.services.buyer;

import application.domain.models.Buyer;
import application.domain.models.PageResult;
import application.domain.ports.out.BuyerRepositoryPort;
import application.domain.ports.in.BuyerInputPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class BuyerService implements BuyerInputPort {
    private final BuyerRepositoryPort buyers;

    public BuyerService(BuyerRepositoryPort buyers) {
        this.buyers = buyers;
    }

    public Buyer register(Buyer buyer) {
        if (buyer == null) {
            throw new IllegalArgumentException("buyer is required");
        }
        requireId(buyer.getId());
        if (buyers.findById(buyer.getId()).isPresent()) {
            throw new IllegalArgumentException("Buyer already exists");
        }
        if (buyers.findByUserId(buyer.getUserId()).isPresent()) {
            throw new IllegalArgumentException("A buyer profile already exists for this user");
        }
        buyers.save(buyer);
        return buyer;
    }

    public Buyer findById(Long buyerId) {
        requireId(buyerId);
        return buyers.findById(buyerId)
                .orElseThrow(() -> new IllegalArgumentException("Buyer not found"));
    }

    public Buyer findByUserId(Long userId) {
        requireId(userId);
        return buyers.findByUserId(userId)
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
    public PageResult<Buyer> findPage(int page, int size) { return buyers.findPage(page, size); }

    private static void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }
}