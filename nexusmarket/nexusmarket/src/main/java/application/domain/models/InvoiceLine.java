package application.domain.models;

import java.math.BigDecimal;

public record InvoiceLine(Long productId, int quantity, BigDecimal unitPrice) { }
