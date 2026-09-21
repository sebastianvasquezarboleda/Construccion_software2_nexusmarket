package application.domain.models;

import application.domain.enums.WarehouseType;

import java.util.Objects;

public record Warehouse(Long id, String name, WarehouseType type) {
    public Warehouse {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name is required");
        }
        Objects.requireNonNull(type, "type is required");
    }
}