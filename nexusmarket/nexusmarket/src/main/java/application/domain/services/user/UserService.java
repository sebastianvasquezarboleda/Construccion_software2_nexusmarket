package application.domain.services.user;

import application.domain.models.User;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    private final Map<Long, User> users = new HashMap<>();

    public User register(User user) {
        requireId(user.getId());
        if (users.putIfAbsent(user.getId(), user) != null) {
            throw new IllegalArgumentException("User already exists");
        }
        return user;
    }

    public User findById(Long userId) {
        User user = users.get(userId);
        if (user == null) {
            throw new IllegalArgumentException("User not found");
        }
        return user;
    }

    public void activate(Long userId) { findById(userId).activate(); }
    public void block(Long userId) { findById(userId).block(); }
    public boolean isActive(Long userId) { return findById(userId).isActive(); }
    public List<User> findAll() { return List.copyOf(users.values()); }

    private static void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }
}