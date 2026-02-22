package com.avanzada.repository;

import com.avanzada.entity.ExampleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ExampleEntityRepository extends JpaRepository<ExampleEntity, Long> {
}
