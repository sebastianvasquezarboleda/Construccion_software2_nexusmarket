package application.services.order;

import application.domain.enums.OrderStatus;
import application.domain.models.Order;
import application.domain.models.OrderItem;
import application.domain.models.InventoryAllocation;
import application.domain.ports.in.InventoryInputPort;
import application.domain.ports.in.OrderInputPort;
import application.domain.ports.in.ProductInputPort;
import application.domain.ports.out.OrderRepositoryPort;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.Objects;
import java.time.Instant;

@Service
public class OrderService implements OrderInputPort {
    private final OrderRepositoryPort orders;
    private final InventoryInputPort inventoryService;
    private final ProductInputPort products;

    @Autowired
    public OrderService(OrderRepositoryPort orders, InventoryInputPort inventoryService, ProductInputPort products) {
        this.orders = Objects.requireNonNull(orders, "orders is required");
        this.inventoryService = Objects.requireNonNull(inventoryService, "inventoryService is required");
        this.products = Objects.requireNonNull(products, "products is required");
    }

    public Order create(Long orderId, Long buyerId) {
        requireId(orderId);
        if (buyerId == null) {
            throw new IllegalArgumentException("buyerId is required");
        }
        if (orders.findById(orderId).isPresent()) {
            throw new IllegalArgumentException("Order already exists");
        }
        Order order = new Order(orderId, buyerId, OrderStatus.CART);
        orders.save(order);
        return order;
    }

    public Order findById(Long orderId) {
        requireId(orderId);
        return orders.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("Order not found"));
    }

    public void addProduct(Long orderId, Long productId, int quantity) {
        requireId(orderId);
        Order order = findById(orderId).copy();
        var product = products.findById(productId);
        if (product.getStatus() != application.domain.enums.ProductStatus.PUBLISHED) {
            throw new IllegalStateException("Only published products can be added to an order");
        }
        order.validateCanAddProduct(productId, quantity, product.getPrice());
        boolean physical = product.isPhysical();
        List<InventoryAllocation> allocations = physical
                ? inventoryService.reserveByProduct(productId, quantity) : List.of();
        try {
            order.addProduct(productId, quantity, product.getPrice(), allocations);
            orders.save(order);
        } catch (RuntimeException ex) {
            if (physical) compensate(ex, () -> inventoryService.returnAllocations(allocations));
            throw ex;
        }
    }

    public void removeProduct(Long orderId, Long productId) {
        requireId(orderId);
        if (productId == null) {
            throw new IllegalArgumentException("productId is required");
        }

        Order order = findById(orderId).copy();
        order.validateCanRemoveProduct(productId);
        List<OrderItem> matchingItems = order.getItems().stream()
                .filter(item -> item.productId().equals(productId))
                .toList();
        boolean physical = isPhysical(productId);
        List<InventoryAllocation> allocations = matchingItems.stream()
                .flatMap(item -> item.allocations().stream())
                .toList();
        boolean hasAllocationDetails = matchingItems.stream().allMatch(item -> !item.allocations().isEmpty());
        if (physical) {
            if (hasAllocationDetails) inventoryService.returnAllocations(allocations);
            else inventoryService.returnForProduct(productId, matchingItems.stream().mapToInt(OrderItem::quantity).sum());
        }
        try {
            order.removeProduct(productId);
            orders.save(order);
        } catch (RuntimeException ex) {
            if (physical) {
                if (hasAllocationDetails) compensate(ex, () -> inventoryService.reserveAllocations(allocations));
                else compensate(ex, () -> inventoryService.reserveByProduct(productId,
                        matchingItems.stream().mapToInt(OrderItem::quantity).sum()));
            }
            throw ex;
        }
    }

    public void updateTotal(Long orderId) { requireId(orderId); update(orderId, Order::updateTotal); }
    public void sendToPayment(Long orderId) { requireId(orderId); update(orderId, Order::sendToPayment); }
    public void markAsPaid(Long orderId) { requireId(orderId); update(orderId, Order::markAsPaid); }
    public void dispatch(Long orderId) { requireId(orderId); update(orderId, Order::dispatch); }
    public void finalizeOrder(Long orderId) { requireId(orderId); update(orderId, Order::finalizeOrder); }
    public void cancel(Long orderId) {
        requireId(orderId);
        cancelOrder(findById(orderId));
    }

    private void cancelOrder(Order snapshot) {
        Order order = snapshot.copy();
        order.validateCanCancel();
        List<OrderItem> items = order.getItems();
        List<OrderItem> returned = new ArrayList<>();
        try {
            for (OrderItem item : items) {
                if (!isPhysical(item.productId())) continue;
                if (item.allocations().isEmpty()) {
                    inventoryService.returnForProduct(item.productId(), item.quantity());
                } else {
                    inventoryService.returnAllocations(item.allocations());
                }
                returned.add(item);
            }
            order.cancel();
            orders.save(order);
        } catch (RuntimeException ex) {
            for (int i = returned.size() - 1; i >= 0; i--) {
                OrderItem item = returned.get(i);
                if (item.allocations().isEmpty()) {
                    compensate(ex, () -> inventoryService.reserveByProduct(item.productId(), item.quantity()));
                } else {
                    compensate(ex, () -> inventoryService.reserveAllocations(item.allocations()));
                }
            }
            throw ex;
        }
    }

    @Override
    public int expireAbandonedOrders(Instant cutoff) {
        if (cutoff == null) {
            throw new IllegalArgumentException("cutoff is required");
        }
        int expired = 0;
        for (Order candidate : orders.findExpiredUnpaid(cutoff)) {
            Order current = findById(candidate.getId());
            if ((current.getStatus() == OrderStatus.CART
                    || current.getStatus() == OrderStatus.PENDING_PAYMENT)
                    && current.getUpdatedAt().isBefore(cutoff)) {
                cancelOrder(current);
                expired++;
            }
        }
        return expired;
    }

    @Override
    public int reserveReturnQuantity(Long orderId, Long productId, int purchasedQuantity,
                                     int previouslyRequestedQuantity, int quantity) {
        return orders.reserveReturnQuantity(
                orderId, productId, purchasedQuantity, previouslyRequestedQuantity, quantity);
    }

    @Override
    public void releaseReturnQuantity(Long orderId, Long productId, int quantity, String returnRequestId) {
        orders.releaseReturnQuantity(orderId, productId, quantity, returnRequestId);
    }

    public List<Order> findAll() { return orders.findAll(); }

    private boolean isPhysical(Long productId) {
        return products.findById(productId).isPhysical();
    }

    private void update(Long orderId, Consumer<Order> action) {
        Order order = findById(orderId).copy();
        action.accept(order);
        orders.save(order);
    }

    private static void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }

    private static void compensate(RuntimeException original, Runnable compensation) {
        try {
            compensation.run();
        } catch (RuntimeException compensationFailure) {
            original.addSuppressed(compensationFailure);
        }
    }
}