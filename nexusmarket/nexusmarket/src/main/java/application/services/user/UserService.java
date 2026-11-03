package application.services.user;

import application.domain.models.User;
import application.domain.ports.in.UserInputPort;
import application.domain.ports.out.UserRepositoryPort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService implements UserInputPort {
    private final UserRepositoryPort users;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepositoryPort users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(User user, String rawPassword) {
        ensureUnique(user);
        if (rawPassword == null || rawPassword.length() < 12) {
            throw new IllegalArgumentException("password must contain at least 12 characters");
        }
        users.save(user, passwordEncoder.encode(rawPassword));
        return user;
    }

    public void changePassword(Long userId, String rawPassword) {
        requireId(userId);
        if (rawPassword == null || rawPassword.length() < 12) {
            throw new IllegalArgumentException("password must contain at least 12 characters");
        }
        users.save(findById(userId), passwordEncoder.encode(rawPassword));
    }

    private void ensureUnique(User user) {
        requireUser(user);
        requireId(user.getId());
        if (users.findById(user.getId()).isPresent()) {
            throw new IllegalArgumentException("User already exists");
        }
        if (users.findByEmail(user.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email is already registered");
        }
        if (users.findByDocumentNumber(user.getDocumentNumber()).isPresent()) {
            throw new IllegalArgumentException("Identity document is already registered");
        }
    }

    public User findById(Long userId) {
        requireId(userId);
        return users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public User findByEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        return users.findByEmail(email.trim())
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