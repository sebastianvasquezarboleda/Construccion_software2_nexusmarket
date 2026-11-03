package application.adapters.out.persistence.entity;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.ArrayList;
import java.util.List;

@Document("buyers")
public class BuyerDocument {
    @Id
    private Long id;
    @Indexed(unique = true, name = "buyers_user_unique")
    private Long userId;
    private String mainAddress;
    private List<String> additionalAddresses = new ArrayList<>();
    private String commercialStatus;

    public BuyerDocument() { }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getMainAddress() { return mainAddress; }
    public void setMainAddress(String mainAddress) { this.mainAddress = mainAddress; }
    public List<String> getAdditionalAddresses() { return additionalAddresses; }
    public void setAdditionalAddresses(List<String> additionalAddresses) {
        this.additionalAddresses = new ArrayList<>(additionalAddresses == null ? List.of() : additionalAddresses);
    }
    public String getCommercialStatus() { return commercialStatus; }
    public void setCommercialStatus(String commercialStatus) { this.commercialStatus = commercialStatus; }
}