package application.adapters.out.memory;

import application.domain.models.User;
import application.domain.models.UserCredential;
import application.domain.ports.out.UserRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryUserRepositoryAdapter implements UserRepositoryPort {
    private final ConcurrentMap<Long, User> users = new ConcurrentHashMap<>();
    private final ConcurrentMap<Long, String> passwordHashes = new ConcurrentHashMap<>();

    @Override
    public void save(User user) { users.put(user.getId(), user); }

    @Override
    public void save(User user, String passwordHash) {
        users.put(user.getId(), user);
        passwordHashes.put(user.getId(), passwordHash);
    }

    @Override
    public Optional<User> findById(Long id) { return Optional.ofNullable(users.get(id)); }

    @Override
    public Optional<User> findByEmail(String email) {
        return users.values().stream().filter(user -> user.getEmail().equalsIgnoreCase(email)).findFirst();
    }

    @Override
    public Optional<User> findByDocumentNumber(String documentNumber) {
        return users.values().stream()
                .filter(user -> user.getDocumentNumber().equalsIgnoreCase(documentNumber))
                .findFirst();
    }

    @Override
    public Optional<UserCredential> findCredentialByEmail(String email) {
        return users.values().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .filter(user -> passwordHashes.containsKey(user.getId()))
                .map(user -> new UserCredential(user.getId(), user.getEmail(),
                        passwordHashes.get(user.getId()), user.getRole(), user.getStatus()))
                .findFirst();
    }

    @Override
    public List<User> findAll() { return List.copyOf(users.values()); }
}