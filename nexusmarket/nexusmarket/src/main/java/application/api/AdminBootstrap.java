package application.api;

import application.domain.enums.Role;
import application.domain.enums.UserStatus;
import application.domain.models.User;
import application.domain.ports.in.UserInputPort;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class AdminBootstrap implements ApplicationRunner {
    private final UserInputPort users;
    private final Environment environment;

    public AdminBootstrap(UserInputPort users, Environment environment) {
        this.users = users;
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        String email = environment.getProperty("NEXUSMARKET_ADMIN_EMAIL", "").trim();
        String password = environment.getProperty("NEXUSMARKET_ADMIN_PASSWORD", "");
        if (email.isEmpty() && password.isEmpty()) return;
        if (email.isEmpty() || password.isEmpty()) {
            throw new IllegalStateException("Both NEXUSMARKET_ADMIN_EMAIL and NEXUSMARKET_ADMIN_PASSWORD are required");
        }
        var existing = users.findAll().stream()
                .filter(user -> user.getEmail().equalsIgnoreCase(email))
                .findFirst();
        if (existing.isPresent()) {
            if (existing.get().getRole() != Role.ADMINISTRATOR) {
                throw new IllegalStateException("The configured bootstrap email already belongs to a non-administrator");
            }
            users.changePassword(existing.get().getId(), password);
            users.activate(existing.get().getId());
            return;
        }

        User administrator = new User(System.currentTimeMillis(), "NexusMarket Administrator",
                email, "BOOTSTRAP-" + email, Role.ADMINISTRATOR, UserStatus.ACTIVE);
        users.register(administrator, password);
    }
}
