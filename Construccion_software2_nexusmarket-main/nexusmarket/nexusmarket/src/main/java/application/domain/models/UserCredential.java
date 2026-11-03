package application.domain.models;

import application.domain.enums.Role;
import application.domain.enums.UserStatus;

public record UserCredential(Long userId, String email, String passwordHash, Role role, UserStatus status) { }
