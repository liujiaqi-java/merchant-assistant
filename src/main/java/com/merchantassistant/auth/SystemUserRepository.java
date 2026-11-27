package com.merchantassistant.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface SystemUserRepository extends JpaRepository<SystemUser, Long> {
    Optional<SystemUser> findByUsernameAndEnabledTrue(String username);
    boolean existsByUsername(String username);
    boolean existsByUsernameIgnoreCase(String username);
}
