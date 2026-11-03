package application.domain.models;

import application.domain.enums.ShipmentStatus;

public class Shipment {
    private final String id;
    private final Long orderId;
    private final String deliveryAddress;
    private ShipmentStatus status;

    public Shipment(String id, Long orderId, String deliveryAddress, ShipmentStatus status) {
        if (id == null || id.isBlank() || orderId == null || status == null) {
            throw new IllegalArgumentException("Shipment fields are required");
        }
        this.id = id;
        this.orderId = orderId;
        this.deliveryAddress = required(deliveryAddress);
        this.status = status;
    }

    public void pack() { transition(ShipmentStatus.CREATED, ShipmentStatus.PACKED); }
    public void dispatch() { transition(ShipmentStatus.PACKED, ShipmentStatus.DISPATCHED); }
    public void deliver() { transition(ShipmentStatus.DISPATCHED, ShipmentStatus.DELIVERED); }

    public String getId() { return id; }
    public Long getOrderId() { return orderId; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public ShipmentStatus getStatus() { return status; }

    private void transition(ShipmentStatus expected, ShipmentStatus next) {
        if (status != expected) throw new IllegalStateException("Invalid shipment transition from " + status + " to " + next);
        status = next;
    }

    private static String required(String value) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException("deliveryAddress is required");
        return value.trim();
    }
}
