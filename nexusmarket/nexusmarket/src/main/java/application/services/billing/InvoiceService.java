package application.services.billing;

import application.domain.enums.OrderStatus;
import application.domain.models.Invoice;
import application.domain.models.InvoiceLine;
import application.domain.models.Order;
import application.domain.ports.in.InvoiceInputPort;
import application.domain.ports.out.InvoiceRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class InvoiceService implements InvoiceInputPort {
    private final InvoiceRepositoryPort invoices;

    public InvoiceService(InvoiceRepositoryPort invoices) {
        this.invoices = Objects.requireNonNull(invoices);
    }

    @Override
    public Invoice createForPaidOrder(Order order) {
        if (order == null || (order.getStatus() != OrderStatus.PAID
                && order.getStatus() != OrderStatus.DISPATCHED
                && order.getStatus() != OrderStatus.FINALIZED)) {
            throw new IllegalStateException("Invoices can only be issued for paid orders");
        }
        return invoices.findByOrderId(order.getId()).orElseGet(() -> {
            List<InvoiceLine> lines = order.getItems().stream()
                    .map(item -> new InvoiceLine(item.productId(), item.quantity(), item.unitPrice()))
                    .toList();
            return invoices.save(new Invoice(UUID.randomUUID().toString(), order.getId(),
                    order.getBuyerId(), lines, order.getTotal(), Instant.now()));
        });
    }

    @Override
    public Invoice findById(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
        return invoices.findById(id).orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
    }

    @Override
    public Invoice findByOrderId(Long orderId) {
        if (orderId == null) throw new IllegalArgumentException("orderId is required");
        return invoices.findByOrderId(orderId).orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
    }

    @Override
    public List<Invoice> findAll() { return invoices.findAll(); }
}
