package com.avanzada.repository;

import com.avanzada.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByIdentifier(String identifier);

    List<User> findByActiveTrue();
}
