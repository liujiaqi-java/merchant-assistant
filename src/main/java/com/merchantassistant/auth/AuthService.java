package com.merchantassistant.auth;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AuthService {
    private final SystemUserRepository repository;
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, RecoveryAttempts> recoveryAttempts = new ConcurrentHashMap<>();
    private final Duration sessionDuration;
    private static final Duration RECOVERY_WINDOW = Duration.ofMinutes(15);
    private static final int MAX_RECOVERY_ATTEMPTS = 5;
    public AuthService(SystemUserRepository repository, @Value("${app.security.session-minutes:120}") long sessionMinutes) {
        this.repository = repository; this.sessionDuration = Duration.ofMinutes(sessionMinutes);
    }
    @PostConstruct @Transactional void seedUsers() {
        createIfMissing("admin", "admin123", UserRole.ADMIN);
        createIfMissing("clerk", "clerk123", UserRole.CLERK);
    }
    @Transactional
    public LoginResponse login(LoginRequest request) {
        SystemUser user = repository.findByUsernameAndEnabledTrue(normalizeUsername(request.username()))
                .filter(value -> PasswordUtil.matches(request.password(), value.getPasswordHash()))
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (PasswordUtil.needsUpgrade(user.getPasswordHash())) { user.setPasswordHash(PasswordUtil.hash(request.password())); repository.save(user); }
        String token = UUID.randomUUID().toString(); sessions.put(token, new Session(user.getId(), Instant.now().plus(sessionDuration)));
        return new LoginResponse(token, UserView.from(user));
    }
    @Transactional
    public UserView register(RegisterRequest request) {
        String username = normalizeUsername(request.username());
        if (repository.existsByUsernameIgnoreCase(username)) throw new IllegalArgumentException("用户名已存在");
        if (request.password().equals(request.recoveryCode())) throw new IllegalArgumentException("找回码不能与登录密码相同");
        SystemUser user = new SystemUser();
        user.setUsername(username);
        user.setPasswordHash(PasswordUtil.hash(request.password()));
        user.setRecoveryCodeHash(PasswordUtil.hash(request.recoveryCode()));
        user.setRole(UserRole.CLERK);
        user.setEnabled(false);
        return UserView.from(repository.save(user));
    }
    @Transactional
    public void recoverPassword(RecoverPasswordRequest request) {
        String username = normalizeUsername(request.username());
        enforceRecoveryLimit(username);
        SystemUser user = repository.findByUsernameAndEnabledTrue(username).orElse(null);
        if (user == null || user.getRecoveryCodeHash() == null
                || !PasswordUtil.matches(request.recoveryCode(), user.getRecoveryCodeHash())) {
            recordRecoveryFailure(username);
            throw new IllegalArgumentException("用户名或找回码错误");
        }
        if (PasswordUtil.matches(request.newPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("新密码不能与当前密码相同");
        }
        user.setPasswordHash(PasswordUtil.hash(request.newPassword()));
        repository.save(user);
        recoveryAttempts.remove(username);
        invalidateUser(user.getId());
    }
    @Transactional
    public void setRecoveryCode(String token, SetRecoveryCodeRequest request) {
        SystemUser user = require(token);
        if (!PasswordUtil.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("当前密码错误");
        }
        if (request.currentPassword().equals(request.recoveryCode())) {
            throw new IllegalArgumentException("找回码不能与登录密码相同");
        }
        user.setRecoveryCodeHash(PasswordUtil.hash(request.recoveryCode()));
        repository.save(user);
    }
    public UserView current(String token) { return UserView.from(require(token)); }
    public void logout(String token) { if (token != null) sessions.remove(token); }
    public void invalidateUser(Long userId) { sessions.entrySet().removeIf(entry -> entry.getValue().userId().equals(userId)); }
    public SystemUser require(String token) {
        Session session = token == null ? null : sessions.get(token);
        if (session == null || session.expiresAt().isBefore(Instant.now())) { if (token != null) sessions.remove(token); throw new IllegalArgumentException("登录已失效，请重新登录"); }
        return repository.findById(session.userId()).filter(user -> Boolean.TRUE.equals(user.getEnabled()))
                .orElseThrow(() -> new IllegalArgumentException("登录已失效，请重新登录"));
    }
    @Transactional
    public void changePassword(String token, ChangePasswordRequest request) {
        SystemUser user = require(token);
        if (!PasswordUtil.matches(request.currentPassword(), user.getPasswordHash())) throw new IllegalArgumentException("当前密码错误");
        if (request.currentPassword().equals(request.newPassword())) throw new IllegalArgumentException("新密码不能与当前密码相同");
        user.setPasswordHash(PasswordUtil.hash(request.newPassword())); repository.save(user);
        sessions.entrySet().removeIf(entry -> entry.getValue().userId().equals(user.getId()));
    }
    private void createIfMissing(String username, String password, UserRole role) {
        if (!repository.existsByUsername(username)) {
            SystemUser user = new SystemUser(); user.setUsername(username); user.setPasswordHash(PasswordUtil.hash(password)); user.setRole(role); user.setEnabled(true); repository.save(user);
        }
    }
    private String normalizeUsername(String username) { return username.trim().toLowerCase(java.util.Locale.ROOT); }
    private void enforceRecoveryLimit(String username) {
        RecoveryAttempts attempts = recoveryAttempts.get(username);
        if (attempts != null && attempts.expiresAt().isAfter(Instant.now()) && attempts.count() >= MAX_RECOVERY_ATTEMPTS) {
            throw new IllegalArgumentException("找回尝试次数过多，请 15 分钟后再试");
        }
        if (attempts != null && attempts.expiresAt().isBefore(Instant.now())) recoveryAttempts.remove(username);
    }
    private void recordRecoveryFailure(String username) {
        recoveryAttempts.compute(username, (key, current) -> {
            Instant now = Instant.now();
            if (current == null || current.expiresAt().isBefore(now)) return new RecoveryAttempts(1, now.plus(RECOVERY_WINDOW));
            return new RecoveryAttempts(current.count() + 1, current.expiresAt());
        });
    }
    private record Session(Long userId, Instant expiresAt) { }
    private record RecoveryAttempts(int count, Instant expiresAt) { }
}
