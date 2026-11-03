package application.domain.ports.in;

import application.domain.models.Shipment;
import application.domain.models.PageResult;

import java.util.List;

public interface ShipmentInputPort {
    Shipment create(Long orderId, String deliveryAddress);
    Shipment findById(String id);
    Shipment findByOrderId(Long orderId);
    List<Shipment> findAll();
    PageResult<Shipment> findPage(int page, int size);
    void pack(String id);
    void dispatch(String id);
    void deliver(String id);
}
