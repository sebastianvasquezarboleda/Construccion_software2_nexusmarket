package application.domain.ports.in;

import application.domain.models.Buyer;

import java.util.List;

public interface BuyerInputPort {
    Buyer register(Buyer buyer);
    Buyer findById(Long buyerId);
    Buyer findByUserId(Long userId);
    void addAddress(Long buyerId, String address);
    void removeAddress(Long buyerId, String address);
    List<Buyer> findAll();
}
