package application.adapters.out.persistence;

import application.adapters.out.persistence.document.BuyerDocument;
import application.adapters.out.persistence.repository.BuyerMongoRepository;
import application.domain.models.Buyer;
import application.domain.ports.out.BuyerRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MongoBuyerRepositoryAdapter implements BuyerRepositoryPort {
    private final BuyerMongoRepository repository;

    public MongoBuyerRepositoryAdapter(BuyerMongoRepository repository) { this.repository = repository; }

    @Override
    public void save(Buyer buyer) {
        BuyerDocument document = new BuyerDocument();
        document.setId(buyer.getId());
        document.setUserId(buyer.getUserId());
        document.setMainAddress(buyer.getMainAddress());
        document.setAdditionalAddresses(buyer.getAdditionalAddresses());
        document.setCommercialStatus(buyer.getCommercialStatus());
        repository.save(document);
    }

    @Override
    public Optional<Buyer> findById(Long id) { return repository.findById(id).map(MongoBuyerRepositoryAdapter::toDomain); }

    @Override
    public List<Buyer> findAll() { return repository.findAll().stream().map(MongoBuyerRepositoryAdapter::toDomain).toList(); }

    private static Buyer toDomain(BuyerDocument document) {
        return new Buyer(document.getId(), document.getUserId(), document.getMainAddress(),
                document.getAdditionalAddresses(), document.getCommercialStatus());
    }
}