package application.domain.models;

import application.domain.enums.OrderStatus;

import java.math.BigDecimal;
import java.util.Map;

public record AdministrativeSummary(Map<OrderStatus, Long> ordersByStatus, BigDecimal grossSales,
                                    int availableInventory, int damagedInventory) {
    public AdministrativeSummary {
        ordersByStatus = Map.copyOf(ordersByStatus);
    }
}
