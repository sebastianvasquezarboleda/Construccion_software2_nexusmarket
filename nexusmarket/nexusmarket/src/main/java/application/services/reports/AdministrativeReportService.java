package application.services.reports;

import application.domain.enums.OrderStatus;
import application.domain.models.AdministrativeSummary;
import application.domain.models.Order;
import application.domain.ports.in.InventoryInputPort;
import application.domain.ports.in.OrderInputPort;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

@Service
public class AdministrativeReportService {
    private final OrderInputPort orders;
    private final InventoryInputPort inventories;

    public AdministrativeReportService(OrderInputPort orders, InventoryInputPort inventories) {
        this.orders = Objects.requireNonNull(orders);
        this.inventories = Objects.requireNonNull(inventories);
    }

    public AdministrativeSummary summary() {
        Map<OrderStatus, Long> counts = new EnumMap<>(OrderStatus.class);
        Arrays.stream(OrderStatus.values()).forEach(status -> counts.put(status, 0L));
        BigDecimal grossSales = BigDecimal.ZERO;
        for (Order order : orders.findAll()) {
            counts.compute(order.getStatus(), (status, count) -> count + 1);
            if (order.getStatus() == OrderStatus.PAID || order.getStatus() == OrderStatus.DISPATCHED
                    || order.getStatus() == OrderStatus.FINALIZED) {
                grossSales = grossSales.add(order.getTotal());
            }
        }
        var inventoryItems = inventories.findAll();
        int available = inventoryItems.stream().mapToInt(item -> item.getQuantity()).reduce(0, Math::addExact);
        int damaged = inventoryItems.stream().mapToInt(item -> item.getDamagedQuantity()).reduce(0, Math::addExact);
        return new AdministrativeSummary(counts, grossSales, available, damaged);
    }
}
