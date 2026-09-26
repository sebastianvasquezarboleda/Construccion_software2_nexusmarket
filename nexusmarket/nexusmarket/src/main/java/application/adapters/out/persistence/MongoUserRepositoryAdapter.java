package application.adapters.out.persistence;

import application.adapters.out.persistence.document.UserDocument;
import application.adapters.out.persistence.repository.UserMongoRepository;
import application.domain.models.User;
import application.domain.ports.out.UserRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MongoUserRepositoryAdapter implements UserRepositoryPort {
    private final UserMongoRepository repository;

    public MongoUserRepositoryAdapter(UserMongoRepository repository) { this.repository = repository; }

    @Override
    public void save(User user) {
        UserDocument document = new UserDocument();
        document.setId(user.getId());
        document.setFullName(user.getFullName());
        document.setEmail(user.getEmail());
        document.setDocumentNumber(user.getDocumentNumber());
        document.setRole(user.getRole());
        document.setStatus(user.getStatus());
        repository.save(document);
    }

    @Override
    public Optional<User> findById(Long id) { return repository.findById(id).map(MongoUserRepositoryAdapter::toDomain); }

    @Override
    public List<User> findAll() { return repository.findAll().stream().map(MongoUserRepositoryAdapter::toDomain).toList(); }

    private static User toDomain(UserDocument document) {
        return new User(document.getId(), document.getFullName(), document.getEmail(),
                document.getDocumentNumber(), document.getRole(), document.getStatus());
    }
}