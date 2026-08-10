package com.nyxeira.homerunner.usermanagement.repositories;

import com.nyxeira.homerunner.usermanagement.model.User;
import com.nyxeira.homerunner.usermanagement.model.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByLogin(String login);
    boolean existsByEmail(String email);
    boolean existsByRole(UserRole role);
    Optional<User> findByLogin(String login);
}
