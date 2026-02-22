package com.avanzada.repository;

import com.avanzada.entity.State;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StateRepository extends JpaRepository<State, Long> {

    Optional<State> findByCode(String code);
}
