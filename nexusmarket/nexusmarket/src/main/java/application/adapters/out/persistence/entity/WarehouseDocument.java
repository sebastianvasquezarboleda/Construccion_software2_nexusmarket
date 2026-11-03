package application.adapters.out.persistence.entity;

import application.domain.enums.WarehouseType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("warehouses")
public class WarehouseDocument {
    @Id
    private Long id;
    private String name;
    private WarehouseType type;
    private Long sellerId;

    public WarehouseDocument() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public WarehouseType getType() { return type; }
    public void setType(WarehouseType type) { this.type = type; }
    public Long getSellerId() { return sellerId; }
    public void setSellerId(Long sellerId) { this.sellerId = sellerId; }
}