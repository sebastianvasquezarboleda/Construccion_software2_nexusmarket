package application.domain.ports.in;

import application.domain.models.User;

import java.util.List;

public interface UserInputPort {
    User register(User user, String rawPassword);
    void changePassword(Long userId, String rawPassword);
    User findById(Long userId);
    User findByEmail(String email);
    void activate(Long userId);
    void block(Long userId);
    boolean isActive(Long userId);
    List<User> findAll();
}
