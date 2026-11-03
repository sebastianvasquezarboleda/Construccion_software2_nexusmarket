package application.domain.ports.in;

import application.domain.models.Inventory;
import application.domain.models.InventoryAllocation;
import application.domain.models.PageResult;

import java.util.List;

public interface InventoryInputPort {
    Inventory register(Inventory inventory);
    Inventory findById(Long inventoryId);
    Inventory findByProductId(Long productId);
    void add(Long inventoryId, int amount);
    void reserve(Long inventoryId, int amount);
    List<InventoryAllocation> reserveByProduct(Long productId, int amount);
    void reserveAllocations(List<InventoryAllocation> allocations);
    void returnAllocations(List<InventoryAllocation> allocations);
    void returnAllocations(List<InventoryAllocation> allocations, String operationReference);
    void confirmSaleAllocations(Long orderId, List<InventoryAllocation> allocations);
    void remove(Long inventoryId, int amount);
    void returnInventory(Long inventoryId, int amount);
    void returnForProduct(Long productId, int amount);
    void returnForProduct(Long productId, int amount, String operationReference);
    void markDamaged(Long inventoryId, int amount);
    void adjust(Long inventoryId, int quantity);
    int getAvailability(Long inventoryId);
    List<Inventory> findAll();
    PageResult<Inventory> findPage(int page, int size);
    PageResult<Inventory> findPageForProducts(List<Long> productIds, int page, int size);
}
