package application.domain.models;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record Invoice(String id, Long orderId, Long buyerId, List<InvoiceLine> items,
                      BigDecimal total, Instant issuedAt) {
    public Invoice {
        if (id == null || id.isBlank() || orderId == null || buyerId == null
                || total == null || issuedAt == null) {
            throw new IllegalArgumentException("Invoice fields are required");
        }
        items = List.copyOf(items);
        if (items.isEmpty()) throw new IllegalArgumentException("An invoice must contain items");
    }
}
