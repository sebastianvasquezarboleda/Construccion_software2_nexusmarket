package application.domain.ports.out;

import application.domain.models.User;
import application.domain.models.UserCredential;

import java.util.List;
import java.util.Optional;

public interface UserRepositoryPort {
    void save(User user);
    void save(User user, String passwordHash);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    Optional<User> findByDocumentNumber(String documentNumber);
    Optional<UserCredential> findCredentialByEmail(String email);
    List<User> findAll();
}