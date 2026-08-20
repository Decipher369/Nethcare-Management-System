package com.nethcare.repository;

import com.nethcare.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/** Lookups the login flow needs. */
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);
}
