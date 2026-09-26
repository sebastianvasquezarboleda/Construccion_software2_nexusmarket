package application.adapters.out.memory;

import application.domain.models.User;
import application.domain.ports.out.UserRepositoryPort;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InMemoryUserRepositoryAdapter implements UserRepositoryPort {
    private final ConcurrentMap<Long, User> users = new ConcurrentHashMap<>();

    @Override
    public void save(User user) { users.put(user.getId(), user); }

    @Override
    public Optional<User> findById(Long id) { return Optional.ofNullable(users.get(id)); }

    @Override
    public List<User> findAll() { return List.copyOf(users.values()); }
}