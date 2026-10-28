package com.tradeguard.user;

import com.tradeguard.common.DuplicateResourceException;
import com.tradeguard.common.InvalidRequestException;
import com.tradeguard.common.ResourceNotFoundException;
import com.tradeguard.user.dto.CreateUserRequest;
import com.tradeguard.user.dto.UserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Service
public class UserService {

    private final AppUserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UserService(AppUserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserResponse create(CreateUserRequest request) {
        if (request.username() == null) {
            throw new InvalidRequestException("帳號不可為空。");
        }
        String username = request.username().toLowerCase(Locale.ROOT);
        if (!username.matches("[a-z0-9._-]{3,50}")) {
            throw new InvalidRequestException("帳號格式錯誤，需為 3 至 50 個英數字或 . _ -。");
        }
        if (request.password() == null || request.password().length() < 12
                || request.password().isBlank()) {
            throw new InvalidRequestException("密碼至少需 12 個字元。");
        }
        if (request.role() == null) {
            throw new InvalidRequestException("帳號角色不可為空。");
        }
        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new InvalidRequestException("密碼 UTF-8 編碼長度不可超過 72 bytes。");
        }
        if (repository.existsByUsername(username)) {
            throw new DuplicateResourceException("帳號已存在: " + username);
        }

        try {
            AppUser user = new AppUser(username, passwordEncoder.encode(request.password()),
                    request.role());
            return UserResponse.from(repository.saveAndFlush(user));
        } catch (DataIntegrityViolationException ex) {
            throw new DuplicateResourceException("帳號已存在: " + username);
        }
    }

    @Transactional(readOnly = true)
    public UserResponse findById(Long id) {
        return repository.findById(id).map(UserResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("找不到帳號 ID: " + id));
    }

    @Transactional(readOnly = true)
    public Page<UserResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(UserResponse::from);
    }
}
