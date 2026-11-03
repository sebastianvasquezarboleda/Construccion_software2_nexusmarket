package application.adapters.out.persistence.entity;

import application.domain.enums.ShipmentStatus;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("shipments")
public class ShipmentDocument {
    @Id
    private String id;
    @Indexed(unique = true, name = "shipments_order_unique")
    private Long orderId;
    private String deliveryAddress;
    private ShipmentStatus status;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getDeliveryAddress() { return deliveryAddress; }
    public void setDeliveryAddress(String deliveryAddress) { this.deliveryAddress = deliveryAddress; }
    public ShipmentStatus getStatus() { return status; }
    public void setStatus(ShipmentStatus status) { this.status = status; }
}
