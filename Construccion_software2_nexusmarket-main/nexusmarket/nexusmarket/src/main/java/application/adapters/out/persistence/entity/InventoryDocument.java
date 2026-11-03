package application.adapters.out.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;

import java.util.ArrayList;
import java.util.List;

@Document("inventories")
public class InventoryDocument {
    @Id
    private Long id;
    @Indexed
    private Long productId;
    private Long warehouseId;
    private int quantity;
    private int damagedQuantity;
    private Long version;
    private List<InventoryMovementDocument> movements = new ArrayList<>();

    public InventoryDocument() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getWarehouseId() { return warehouseId; }
    public void setWarehouseId(Long warehouseId) { this.warehouseId = warehouseId; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getDamagedQuantity() { return damagedQuantity; }
    public void setDamagedQuantity(int damagedQuantity) { this.damagedQuantity = damagedQuantity; }
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
    public List<InventoryMovementDocument> getMovements() { return movements; }
    public void setMovements(List<InventoryMovementDocument> movements) {
        this.movements = movements == null ? new ArrayList<>() : new ArrayList<>(movements);
    }
}