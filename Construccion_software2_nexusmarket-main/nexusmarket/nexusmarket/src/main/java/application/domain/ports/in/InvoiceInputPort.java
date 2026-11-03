package application.domain.ports.in;

import application.domain.models.Invoice;
import application.domain.models.Order;
import application.domain.models.PageResult;

import java.util.List;

public interface InvoiceInputPort {
    Invoice createForPaidOrder(Order order);
    Invoice findById(String id);
    Invoice findByOrderId(Long orderId);
    List<Invoice> findAll();
    PageResult<Invoice> findPage(int page, int size);
}
