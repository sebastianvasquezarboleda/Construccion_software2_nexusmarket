package application.services.returns;

import application.domain.enums.OrderStatus;
import application.domain.enums.ReturnStatus;
import application.domain.models.Order;
import application.domain.models.InventoryAllocation;
import application.domain.models.OrderItem;
import application.domain.models.ReturnRequest;
import application.domain.ports.in.InventoryInputPort;
import application.domain.ports.in.OrderInputPort;
import application.domain.ports.in.ReturnInputPort;
import application.domain.ports.out.ReturnRequestRepositoryPort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class ReturnService implements ReturnInputPort {
    private final ReturnRequestRepositoryPort returns;
    private final OrderInputPort orders;
    private final InventoryInputPort inventory;

    public ReturnService(ReturnRequestRepositoryPort returns, OrderInputPort orders, InventoryInputPort inventory) {
        this.returns = Objects.requireNonNull(returns);
        this.orders = Objects.requireNonNull(orders);
        this.inventory = Objects.requireNonNull(inventory);
    }

    @Override
    public ReturnRequest request(Long orderId, Long productId, int quantity, String reason) {
        if (orderId == null || productId == null) throw new IllegalArgumentException("orderId and productId are required");
        if (quantity <= 0) throw new IllegalArgumentException("quantity must be positive");
        Order order = orders.findById(orderId);
        if (order.getStatus() != OrderStatus.FINALIZED) {
            throw new IllegalStateException("Returns can only be requested for finalized orders");
        }
        int purchased = order.getItems().stream()
                .filter(item -> item.productId().equals(productId))
                .mapToInt(item -> item.quantity())
                .sum();
        if (purchased == 0) {
            throw new IllegalArgumentException("Return quantity exceeds the unreturned purchased quantity");
        }
        int previouslyRequested = returns.findByOrderId(orderId).stream()
                .filter(item -> item.getProductId().equals(productId))
                .filter(item -> item.getStatus() != ReturnStatus.REJECTED)
                .mapToInt(ReturnRequest::getQuantity)
                .sum();
        int alreadyRequested = orders.reserveReturnQuantity(
                orderId, productId, purchased, previouslyRequested, quantity);
        String requestId = UUID.randomUUID().toString();
        try {
            List<OrderItem> matchingItems = order.getItems().stream()
                    .filter(item -> item.productId().equals(productId))
                    .toList();
            List<InventoryAllocation> allocations = allocationsForReturn(matchingItems, alreadyRequested, quantity);
            ReturnRequest request = new ReturnRequest(requestId, orderId, order.getBuyerId(),
                    productId, quantity, reason, ReturnStatus.REQUESTED, Instant.now(), allocations);
            return returns.save(request);
        } catch (RuntimeException ex) {
            try {
                if (returns.findById(requestId).isEmpty()) {
                    orders.releaseReturnQuantity(orderId, productId, quantity, requestId);
                }
            } catch (RuntimeException reconciliationFailure) {
                ex.addSuppressed(reconciliationFailure);
            }
            throw ex;
        }
    }

    @Override
    public ReturnRequest findById(String id) {
        if (id == null || id.isBlank()) throw new IllegalArgumentException("id is required");
        return returns.findById(id).orElseThrow(() -> new IllegalArgumentException("Return request not found"));
    }

    @Override
    public List<ReturnRequest> findAll() { return returns.findAll(); }

    @Override
    public void approve(String id) {
        ReturnRequest request = findById(id);
        if (request.getStatus() == ReturnStatus.REQUESTED) {
            returns.transitionStatus(id, ReturnStatus.REQUESTED, ReturnStatus.APPROVED);
            request = findById(id);
        }
        if (request.getStatus() != ReturnStatus.APPROVED && request.getStatus() != ReturnStatus.REFUNDED) {
            throw new IllegalStateException("Only requested returns can be approved");
        }
        restoreInventory(request);
    }

    @Override
    public void reject(String id) {
        ReturnRequest request = findById(id);
        if (request.getStatus() == ReturnStatus.REQUESTED) {
            returns.transitionStatus(id, ReturnStatus.REQUESTED, ReturnStatus.REJECTED);
            request = findById(id);
        }
        if (request.getStatus() != ReturnStatus.REJECTED) {
            throw new IllegalStateException("Only requested returns can be rejected");
        }
        orders.releaseReturnQuantity(request.getOrderId(), request.getProductId(),
                request.getQuantity(), request.getId());
    }

    @Override
    public void recordRefund(String id) {
        ReturnRequest request = findById(id);
        if (request.getStatus() != ReturnStatus.APPROVED && request.getStatus() != ReturnStatus.REFUNDED) {
            throw new IllegalStateException("Only approved returns can be refunded");
        }
        restoreInventory(request);
        if (request.getStatus() == ReturnStatus.APPROVED) {
            returns.transitionStatus(id, ReturnStatus.APPROVED, ReturnStatus.REFUNDED);
            if (findById(id).getStatus() != ReturnStatus.REFUNDED) {
                throw new IllegalStateException("Return status changed before refund was recorded");
            }
        }
    }

    private void restoreInventory(ReturnRequest request) {
        if (request.getAllocations().isEmpty()) {
            inventory.returnForProduct(request.getProductId(), request.getQuantity(), request.getId());
        } else {
            inventory.returnAllocations(request.getAllocations(), request.getId());
        }
    }

    private List<InventoryAllocation> allocationsForReturn(List<OrderItem> items, int alreadyRequested, int quantity) {
        List<InventoryAllocation> purchasedAllocations = items.stream()
                .flatMap(item -> item.allocations().stream())
                .toList();
        if (purchasedAllocations.isEmpty() || items.stream().anyMatch(item -> item.allocations().isEmpty())) {
            return List.of(new InventoryAllocation(inventory.findByProductId(items.get(0).productId()).getId(), quantity));
        }
        int toSkip = alreadyRequested;
        int remaining = quantity;
        List<InventoryAllocation> requestedAllocations = new java.util.ArrayList<>();
        for (InventoryAllocation purchased : purchasedAllocations) {
            int skipped = Math.min(toSkip, purchased.quantity());
            toSkip -= skipped;
            int available = purchased.quantity() - skipped;
            int allocated = Math.min(remaining, available);
            if (allocated > 0) {
                requestedAllocations.add(new InventoryAllocation(purchased.inventoryId(), allocated));
                remaining -= allocated;
            }
            if (remaining == 0) break;
        }
        if (remaining != 0) {
            throw new IllegalArgumentException("Return quantity exceeds allocated inventory");
        }
        return List.copyOf(requestedAllocations);
    }
}
