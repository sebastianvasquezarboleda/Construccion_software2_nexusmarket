package application.services.shipping;

import application.domain.enums.OrderStatus;
import application.domain.enums.ProductType;
import application.domain.enums.ShipmentStatus;
import application.domain.models.Order;
import application.domain.models.Shipment;
import application.domain.ports.in.OrderInputPort;
import application.domain.ports.in.ProductInputPort;
import application.domain.ports.in.ShipmentInputPort;
import application.domain.ports.out.ShipmentRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Consumer;

@Service
public class ShipmentService implements ShipmentInputPort {
    private final ShipmentRepositoryPort shipments;
    private final OrderInputPort orders;
    private final ProductInputPort products;

    public ShipmentService(ShipmentRepositoryPort shipments, OrderInputPort orders, ProductInputPort products) {
        this.shipments = Objects.requireNonNull(shipments);
        this.orders = Objects.requireNonNull(orders);
        this.products = Objects.requireNonNull(products);
    }

    @Override
    public Shipment create(Long orderId, String deliveryAddress) {
        if (orderId == null) throw new IllegalArgumentException("orderId is required");
        Order order = orders.findById(orderId);
        if (order.getStatus() != OrderStatus.PAID) throw new IllegalStateException("Only paid orders can be shipped");
        if (order.getItems().stream().noneMatch(item -> products.findById(item.productId()).getType() == ProductType.PHYSICAL)) {
            throw new IllegalArgumentException("Shipments are only available for orders containing physical products");
        }
        return shipments.findByOrderId(orderId).orElseGet(() ->
                shipments.save(new Shipment(UUID.randomUUID().toString(), orderId, deliveryAddress, ShipmentStatus.CREATED)));
    }

    @Override
    public Shipment findById(String id) { return shipments.findById(requireId(id)).orElseThrow(() -> new IllegalArgumentException("Shipment not found")); }

    @Override
    public Shipment findByOrderId(Long orderId) {
        if (orderId == null) throw new IllegalArgumentException("orderId is required");
        return shipments.findByOrderId(orderId).orElseThrow(() -> new IllegalArgumentException("Shipment not found"));
    }

    @Override
    public List<Shipment> findAll() { return shipments.findAll(); }

    @Override
    public void pack(String id) { update(id, Shipment::pack); }

    @Override
    public void dispatch(String id) { update(id, Shipment::dispatch); }

    @Override
    public void deliver(String id) { update(id, Shipment::deliver); }

    private void update(String id, Consumer<Shipment> action) {
        Shipment shipment = findById(id);
        action.accept(shipment);
        shipments.save(shipment);
    }

    private static String requireId(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
        return id;
    }
}
