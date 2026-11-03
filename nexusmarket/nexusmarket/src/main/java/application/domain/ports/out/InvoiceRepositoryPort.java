package application.domain.ports.out;

import application.domain.models.Invoice;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepositoryPort {
    Invoice save(Invoice invoice);
    Optional<Invoice> findById(String id);
    Optional<Invoice> findByOrderId(Long orderId);
    List<Invoice> findAll();
}
