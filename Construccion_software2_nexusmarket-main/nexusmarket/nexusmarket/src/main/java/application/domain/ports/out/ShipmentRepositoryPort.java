package application.domain.ports.out;

import application.domain.models.Shipment;

import java.util.Optional;

public interface ShipmentRepositoryPort extends PagedRepositoryPort<Shipment> {
    Shipment save(Shipment shipment);
    Optional<Shipment> findById(String id);
    Optional<Shipment> findByOrderId(Long orderId);
}
