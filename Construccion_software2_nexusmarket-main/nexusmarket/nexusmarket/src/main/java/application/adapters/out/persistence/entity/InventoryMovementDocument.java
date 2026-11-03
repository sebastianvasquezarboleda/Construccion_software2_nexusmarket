package application.adapters.out.persistence.entity;

import application.domain.enums.InventoryMovementType;

import java.time.Instant;

public class InventoryMovementDocument {
    private InventoryMovementType type;
    private int quantity;
    private int resultingQuantity;
    private Instant occurredAt;
    private Long referenceId;
    private String operationReference;

    public InventoryMovementDocument() { }

    public InventoryMovementType getType() { return type; }
    public void setType(InventoryMovementType type) { this.type = type; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getResultingQuantity() { return resultingQuantity; }
    public void setResultingQuantity(int resultingQuantity) { this.resultingQuantity = resultingQuantity; }
    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant occurredAt) { this.occurredAt = occurredAt; }
    public Long getReferenceId() { return referenceId; }
    public void setReferenceId(Long referenceId) { this.referenceId = referenceId; }
    public String getOperationReference() { return operationReference; }
    public void setOperationReference(String operationReference) { this.operationReference = operationReference; }
}
