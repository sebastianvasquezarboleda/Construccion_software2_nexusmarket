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
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
        this.id = id;
        this.userId = userId;
        this.mainAddress = required(mainAddress, "mainAddress");
        this.additionalAddresses = normalizeAddresses(additionalAddresses);
        this.commercialStatus = required(commercialStatus, "commercialStatus");
    }

    public void addAddress(String address) {
        String value = required(address, "address");
        if (!additionalAddresses.contains(value)) {
            additionalAddresses.add(value);
        }
    }

    public void removeAddress(String address) {
        String value = required(address, "address");
        if (!additionalAddresses.remove(value)) {
            throw new IllegalArgumentException("Address does not exist");
        }
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getMainAddress() { return mainAddress; }
    public List<String> getAdditionalAddresses() { return List.copyOf(additionalAddresses); }
    public String getCommercialStatus() { return commercialStatus; }

    private static List<String> normalizeAddresses(List<String> addresses) {
        List<String> normalized = new ArrayList<>();
        if (addresses == null) {
            return normalized;
        }

        for (String address : addresses) {
            String value = required(address, "address");
            if (!normalized.contains(value)) {
                normalized.add(value);
            }
        }
        return normalized;
    }

    private static String required(String value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " is required");
        }

        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return normalized;
    }
}