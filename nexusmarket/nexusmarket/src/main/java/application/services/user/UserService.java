package application.services.user;

import application.domain.models.User;
import application.domain.ports.out.UserRepositoryPort;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {
    private final UserRepositoryPort users;

    public UserService(UserRepositoryPort users) {
        this.users = users;
    }

    public User register(User user) {
        requireUser(user);
        requireId(user.getId());
        if (users.findById(user.getId()).isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }
        users.save(user);
        return user;
    }

    public User findById(Long userId) {
        requireId(userId);
        return users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public void activate(Long userId) {
        requireId(userId);
        User user = findById(userId);
        user.activate();
        users.save(user);
    }

    public void block(Long userId) {
        requireId(userId);
        User user = findById(userId);
        user.block();
        users.save(user);
    }

    public boolean isActive(Long userId) {
        requireId(userId);
        return findById(userId).isActive();
    }
    public List<User> findAll() { return users.findAll(); }

    private static void requireUser(User user) {
        if (user == null) {
            throw new IllegalArgumentException("user is required");
        }
    }

    private static void requireId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("id is required");
        }
    }
}