package application.domain.models;

import java.util.ArrayList;
import java.util.List;

public class Buyer {
    private final Long id;
    private final Long userId;
    private final String mainAddress;
    private final List<String> additionalAddresses;
    private final String commercialStatus;

    public Buyer(Long id, Long userId, String mainAddress,
                 List<String> additionalAddresses, String commercialStatus) {
        this.id = id;
        this.userId = userId;
        this.mainAddress = required(mainAddress, "mainAddress");
        this.additionalAddresses = new ArrayList<>(additionalAddresses == null
                ? List.of() : additionalAddresses);
        this.commercialStatus = commercialStatus;
    }

    public void addAddress(String address) {
        String value = required(address, "address");
        if (!additionalAddresses.contains(value)) {
            additionalAddresses.add(value);
        }
    }

    public void removeAddress(String address) {
        if (!additionalAddresses.remove(address)) {
            throw new IllegalArgumentException("Address does not exist");
        }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getMainAddress() { return mainAddress; }
    public List<String> getAdditionalAddresses() { return List.copyOf(additionalAddresses); }
    public String getCommercialStatus() { return commercialStatus; }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}