package application.domain.models;

import application.domain.enums.WarehouseType;

public record Warehouse(Long id, String name, WarehouseType type, Long sellerId) {
    public Warehouse(Long id, String name, WarehouseType type) {
        this(id, name, type, null);
    }

    public Warehouse {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (type == WarehouseType.SELLER && sellerId == null) {
            throw new IllegalArgumentException("sellerId is required for seller warehouses");
        }
        if (type == WarehouseType.MARKETPLACE && sellerId != null) {
            throw new IllegalArgumentException("Marketplace warehouses cannot have a sellerId");
        }
    }
}