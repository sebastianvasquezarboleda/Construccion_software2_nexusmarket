package application.domain.ports.out;

import application.domain.models.Invoice;

import java.util.Optional;

public interface InvoiceRepositoryPort extends PagedRepositoryPort<Invoice> {
    Invoice save(Invoice invoice);
    Optional<Invoice> findById(String id);
    Optional<Invoice> findByOrderId(Long orderId);
}
