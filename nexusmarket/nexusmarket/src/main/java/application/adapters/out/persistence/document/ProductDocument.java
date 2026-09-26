package application.adapters.out.persistence.document;

import application.domain.enums.ProductStatus;
import application.domain.enums.ProductType;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document("products")
public class ProductDocument {
    @Id
    private Long id;
    private String name;
    private String description;
    private ProductType type;
    private List<String> variants = new ArrayList<>();
    private ProductStatus status;

    public ProductDocument() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public ProductType getType() { return type; }
    public void setType(ProductType type) { this.type = type; }
    public List<String> getVariants() { return variants; }
    public void setVariants(List<String> variants) { this.variants = new ArrayList<>(variants); }
    public ProductStatus getStatus() { return status; }
    public void setStatus(ProductStatus status) { this.status = status; }
}