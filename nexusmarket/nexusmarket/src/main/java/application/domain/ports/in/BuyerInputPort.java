package application.domain.ports.in;

import application.domain.models.Buyer;
import application.domain.models.PageResult;

import java.util.List;

public interface BuyerInputPort {
    Buyer register(Buyer buyer);
    Buyer findById(Long buyerId);
    Buyer findByUserId(Long userId);
    void addAddress(Long buyerId, String address);
    void removeAddress(Long buyerId, String address);
    List<Buyer> findAll();
    PageResult<Buyer> findPage(int page, int size);
}
