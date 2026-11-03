package application.adapters.out.persistence;

import application.adapters.out.persistence.entity.InvoiceDocument;
import application.adapters.out.persistence.entity.InvoiceLineDocument;
import application.adapters.out.persistence.repository.InvoiceMongoRepository;
import application.domain.models.Invoice;
import application.domain.models.InvoiceLine;
import application.domain.ports.out.InvoiceRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MongoInvoiceRepositoryAdapter implements InvoiceRepositoryPort {
    private final InvoiceMongoRepository repository;

    public MongoInvoiceRepositoryAdapter(InvoiceMongoRepository repository) { this.repository = repository; }

    @Override
    public Invoice save(Invoice invoice) {
        InvoiceDocument document = new InvoiceDocument();
        document.setId(invoice.id());
        document.setOrderId(invoice.orderId());
        document.setBuyerId(invoice.buyerId());
        document.setItems(invoice.items().stream().map(MongoInvoiceRepositoryAdapter::toDocument).toList());
        document.setTotal(invoice.total());
        document.setIssuedAt(invoice.issuedAt());
        return toDomain(repository.save(document));
    }

    @Override
    public Optional<Invoice> findById(String id) { return repository.findById(id).map(MongoInvoiceRepositoryAdapter::toDomain); }

    @Override
    public Optional<Invoice> findByOrderId(Long orderId) { return repository.findByOrderId(orderId).map(MongoInvoiceRepositoryAdapter::toDomain); }

    @Override
    public List<Invoice> findAll() { return repository.findAll().stream().map(MongoInvoiceRepositoryAdapter::toDomain).toList(); }

    private static InvoiceDocument toDocument(Invoice invoice) {
        InvoiceDocument document = new InvoiceDocument();
        document.setId(invoice.id());
        document.setOrderId(invoice.orderId());
        document.setBuyerId(invoice.buyerId());
        document.setItems(invoice.items().stream().map(MongoInvoiceRepositoryAdapter::toDocument).toList());
        document.setTotal(invoice.total());
        document.setIssuedAt(invoice.issuedAt());
        return document;
    }

    private static InvoiceLineDocument toDocument(InvoiceLine line) {
        InvoiceLineDocument document = new InvoiceLineDocument();
        document.setProductId(line.productId());
        document.setQuantity(line.quantity());
        document.setUnitPrice(line.unitPrice());
        return document;
    }

    private static Invoice toDomain(InvoiceDocument document) {
        return new Invoice(document.getId(), document.getOrderId(), document.getBuyerId(),
                document.getItems().stream().map(item -> new InvoiceLine(item.getProductId(),
                        item.getQuantity(), item.getUnitPrice())).toList(), document.getTotal(), document.getIssuedAt());
    }
}
