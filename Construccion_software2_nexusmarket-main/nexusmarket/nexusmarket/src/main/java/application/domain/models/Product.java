package application.domain.models;

import application.domain.enums.ProductStatus;
import application.domain.enums.ProductType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class Product {
    private final Long id;
    private final Long sellerId;
    private final String name;
    private final String description;
    private BigDecimal price;
    private final ProductType type;
    private final List<String> variants;
    private ProductStatus status;

    public Product(Long id, String name, String description, ProductType type,
                   List<String> variants, ProductStatus status) {
        this(id, 0L, name, description, type, variants, status, BigDecimal.ZERO);
    }

    public Product(Long id, Long sellerId, String name, String description, ProductType type,
                   List<String> variants, ProductStatus status) {
        this(id, sellerId, name, description, type, variants, status, BigDecimal.ZERO);
    }

    public Product(Long id, Long sellerId, String name, String description, ProductType type,
                   List<String> variants, ProductStatus status, BigDecimal price) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (sellerId == null) {
            throw new IllegalArgumentException("sellerId is required");
        }
        this.id = id;
        this.sellerId = sellerId;
        this.name = required(name, "name");
        this.description = required(description, "description");
        this.price = validPrice(price);
        if (type == null) {
            throw new IllegalArgumentException("type is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        this.type = type;
        this.variants = new ArrayList<>(variants == null ? List.of() : variants);
        this.status = status;
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

    public void updatePrice(BigDecimal price) {
        ensureModifiable();
        this.price = validPrice(price);
    }

    public boolean isPhysical() { return type == ProductType.PHYSICAL; }
    public boolean isDigital() { return type == ProductType.DIGITAL; }

    public Long getId() { return id; }
    public Long getSellerId() { return sellerId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
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

    private static BigDecimal validPrice(BigDecimal price) {
        if (price == null || price.signum() < 0) {
            throw new IllegalArgumentException("price must be zero or greater");
        }
        return price;
    }
}