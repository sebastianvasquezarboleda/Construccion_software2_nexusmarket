package application.domain.models;

import application.domain.enums.InventoryMovementType;

import java.time.Instant;
import java.util.Objects;

public record InventoryMovement(
        InventoryMovementType type,
        int quantity,
        int resultingQuantity,
        Instant occurredAt,
        Long referenceId,
        String operationReference) {

    public InventoryMovement(InventoryMovementType type, int quantity, int resultingQuantity, Instant occurredAt) {
        this(type, quantity, resultingQuantity, occurredAt, null, null);
    }

    public InventoryMovement(InventoryMovementType type, int quantity, int resultingQuantity,
                             Instant occurredAt, Long referenceId) {
        this(type, quantity, resultingQuantity, occurredAt, referenceId, null);
    }

    public InventoryMovement {
        Objects.requireNonNull(type, "type is required");
        if (quantity < 0) {
            throw new IllegalArgumentException("quantity cannot be negative");
        }
        if (resultingQuantity < 0) {
            throw new IllegalArgumentException("resultingQuantity cannot be negative");
        }
        Objects.requireNonNull(occurredAt, "occurredAt is required");
    }
}
