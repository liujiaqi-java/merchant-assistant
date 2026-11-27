package com.merchantassistant.auth;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
public class UserManagementService {
    private final SystemUserRepository repository;
    private final AuthService authService;
    public UserManagementService(SystemUserRepository repository, AuthService authService) { this.repository = repository; this.authService = authService; }
    @Transactional(readOnly = true) public List<UserView> list() { return repository.findAll(Sort.by("id")).stream().map(UserView::from).toList(); }
    @Transactional public UserView create(CreateUserRequest request) {
        String username = request.username().trim();
        if (repository.existsByUsername(username)) throw new IllegalArgumentException("用户名已存在: " + username);
        SystemUser user = new SystemUser(); user.setUsername(username); user.setPasswordHash(PasswordUtil.hash(request.password())); user.setRole(request.role()); user.setEnabled(true);
        return UserView.from(repository.save(user));
    }
    @Transactional public UserView status(Long id, Long operatorId, UserStatusRequest request) {
        SystemUser user = get(id);
        if (id.equals(operatorId) && !request.enabled()) throw new IllegalArgumentException("不能停用当前登录账号");
        user.setEnabled(request.enabled()); UserView result = UserView.from(repository.save(user)); if (!request.enabled()) authService.invalidateUser(id); return result;
    }
    @Transactional public void resetPassword(Long id, ResetPasswordRequest request) {
        SystemUser user = get(id); user.setPasswordHash(PasswordUtil.hash(request.password())); repository.save(user); authService.invalidateUser(id);
    }
    private SystemUser get(Long id) { return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("用户不存在，id=" + id)); }
}
