package application.domain.models;

import application.domain.enums.Role;
import application.domain.enums.UserStatus;

import java.util.Locale;

public class User {
    private final Long id;
    private final String fullName;
    private final String email;
    private final String documentNumber;
    private final Role role;
    private UserStatus status;

    public User(Long id, String fullName, String email, String documentNumber,
                Role role, UserStatus status) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        this.id = id;
        this.fullName = required(fullName, "fullName");
        this.email = required(email, "email").toLowerCase(Locale.ROOT);
        this.documentNumber = required(documentNumber, "documentNumber").toUpperCase(Locale.ROOT);
        if (role == null) {
            throw new IllegalArgumentException("role is required");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        this.role = role;
        this.status = status;
    }

    public void activate() {
        status = UserStatus.ACTIVE;
    }

    public void block() {
        status = UserStatus.BLOCKED;
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE;
    }

    public Long getId() { return id; }
    public String getFullName() { return fullName; }
    public String getEmail() { return email; }
    public String getDocumentNumber() { return documentNumber; }
    public Role getRole() { return role; }
    public UserStatus getStatus() { return status; }

    private static String required(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " is required");
        }
        return value;
    }
}