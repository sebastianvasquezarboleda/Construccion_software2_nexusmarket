package application.domain.models;

import application.domain.enums.ProductStatus;
import application.domain.enums.ProductType;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Product {
    private final Long id;
    private final String name;
    private final String description;
    private final ProductType type;
    private final List<String> variants;
    private ProductStatus status;

    public Product(Long id, String name, String description, ProductType type,
                   List<String> variants, ProductStatus status) {
        this.id = id;
        this.name = required(name, "name");
        this.description = required(description, "description");
        this.type = Objects.requireNonNull(type, "type is required");
        this.variants = new ArrayList<>(variants == null ? List.of() : variants);
        this.status = Objects.requireNonNull(status, "status is required");
    }

    public void publish() {
        if (status == ProductStatus.DISCONTINUED) {
            throw new IllegalStateException("A discontinued product cannot be published");
        }
        status = ProductStatus.PUBLISHED;
    }

    public void suspend() {
        ensureModifiable();
        status = ProductStatus.SUSPENDED;
    }

    public void discontinue() {
        status = ProductStatus.DISCONTINUED;
    }

    public void addVariant(String variant) {
        ensureModifiable();
        String value = required(variant, "variant");
        if (!variants.contains(value)) {
            variants.add(value);
        }
    }

    public boolean isPhysical() { return type == ProductType.PHYSICAL; }
    public boolean isDigital() { return type == ProductType.DIGITAL; }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ProductType getType() { return type; }
    public List<String> getVariants() { return List.copyOf(variants); }
    public ProductStatus getStatus() { return status; }

    private void ensureModifiable() {
        if (status == ProductStatus.DISCONTINUED) {
            throw new IllegalStateException("A discontinued product cannot be modified");
        }
    }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}