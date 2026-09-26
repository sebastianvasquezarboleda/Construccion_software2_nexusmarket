package application.domain.ports.out;

import application.domain.models.User;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    void save(User user);
    Optional<User> findById(Long id);
    List<User> findAll();
}