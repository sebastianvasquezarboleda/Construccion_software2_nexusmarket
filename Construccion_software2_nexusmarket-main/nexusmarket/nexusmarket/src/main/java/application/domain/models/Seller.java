package application.domain.models;

public record Seller(Long id, Long userId) {
    public Seller {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
        if (userId == null) {
            throw new IllegalArgumentException("userId is required");
        }
    }
}