package application.adapters.out.persistence;

import application.adapters.out.persistence.entity.UserDocument;
import application.adapters.out.persistence.repository.UserMongoRepository;
import application.domain.models.User;
import application.domain.models.UserCredential;
import application.domain.models.PageResult;
import application.domain.ports.out.UserRepositoryPort;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MongoUserRepositoryAdapter implements UserRepositoryPort {
    private final UserMongoRepository repository;

    public MongoUserRepositoryAdapter(UserMongoRepository repository) { this.repository = repository; }

    @Override
    public void save(User user) {
        UserDocument document = repository.findById(user.getId()).orElseGet(UserDocument::new);
        updateDocument(document, user);
        repository.save(document);
    }

    @Override
    public void save(User user, String passwordHash) {
        UserDocument document = new UserDocument();
        updateDocument(document, user);
        document.setPasswordHash(passwordHash);
        repository.save(document);
    }

    @Override
    public Optional<User> findById(Long id) { return repository.findById(id).map(MongoUserRepositoryAdapter::toDomain); }

    @Override
    public Optional<User> findByEmail(String email) {
        return repository.findByEmailIgnoreCase(email).map(MongoUserRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<User> findByDocumentNumber(String documentNumber) {
        return repository.findByDocumentNumberIgnoreCase(documentNumber).map(MongoUserRepositoryAdapter::toDomain);
    }

    @Override
    public Optional<UserCredential> findCredentialByEmail(String email) {
        return repository.findByEmailIgnoreCase(email)
                .filter(document -> document.getPasswordHash() != null)
                .map(document -> new UserCredential(document.getId(), document.getEmail(),
                        document.getPasswordHash(), document.getRole(), document.getStatus()));
    }

    @Override
    public List<User> findAll() { return repository.findAll().stream().map(MongoUserRepositoryAdapter::toDomain).toList(); }

    @Override
    public PageResult<User> findPage(int page, int size) {
        PageResult.validate(page, size);
        var result = repository.findAll(PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id")));
        return new PageResult<>(result.getContent().stream().map(MongoUserRepositoryAdapter::toDomain).toList(),
                page, size, result.getTotalElements());
    }

    private static void updateDocument(UserDocument document, User user) {
        document.setId(user.getId());
        document.setFullName(user.getFullName());
        document.setEmail(user.getEmail());
        document.setDocumentNumber(user.getDocumentNumber());
        document.setRole(user.getRole());
        document.setStatus(user.getStatus());
    }

    private static User toDomain(UserDocument document) {
        return new User(document.getId(), document.getFullName(), document.getEmail(),
                document.getDocumentNumber(), document.getRole(), document.getStatus());
    }
}