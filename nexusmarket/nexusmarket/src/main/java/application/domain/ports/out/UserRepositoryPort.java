package application.domain.ports.out;

import application.domain.models.User;
import application.domain.models.UserCredential;

import java.util.Optional;

public interface UserRepositoryPort extends PagedRepositoryPort<User> {
    void save(User user);
    void save(User user, String passwordHash);
    Optional<User> findById(Long id);
    Optional<User> findByEmail(String email);
    Optional<User> findByDocumentNumber(String documentNumber);
    Optional<UserCredential> findCredentialByEmail(String email);
}