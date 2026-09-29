package com.nethcare.repository;

import com.nethcare.model.User;
import com.nethcare.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/** Lookups the login flow needs. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    List<User> findAllByOrderByUsernameAsc();

    List<User> findByRoleOrderByFullNameAsc(Role role);

    long countByRoleAndIsActiveTrue(Role role);
}
