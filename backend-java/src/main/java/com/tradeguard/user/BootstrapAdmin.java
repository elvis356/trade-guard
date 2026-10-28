package com.tradeguard.user;

import com.tradeguard.user.dto.CreateUserRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class BootstrapAdmin implements ApplicationRunner {

    private final AppUserRepository repository;
    private final UserService userService;
    private final String username;
    private final String password;

    public BootstrapAdmin(AppUserRepository repository, UserService userService,
                          @Value("${tradeguard.bootstrap.admin.username:}") String username,
                          @Value("${tradeguard.bootstrap.admin.password:}") String password) {
        this.repository = repository;
        this.userService = userService;
        this.username = username;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (username.isBlank() && password.isBlank()) {
            return;
        }
        if (username.isBlank() || password.isBlank()) {
            throw new IllegalStateException(
                    "Bootstrap admin username and password must both be configured.");
        }
        if (password.length() < 12 || password.getBytes(java.nio.charset.StandardCharsets.UTF_8)
                .length > 72) {
            throw new IllegalStateException(
                    "Bootstrap admin password must be 12-72 UTF-8 bytes long.");
        }
        String normalizedUsername = username.toLowerCase(Locale.ROOT);
        if (!repository.existsByUsername(normalizedUsername)) {
            userService.create(new CreateUserRequest(normalizedUsername, password, Role.ADMIN));
        }
    }
}
