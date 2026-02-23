package com.avanzada.repository;

import com.avanzada.entity.Priority;
import com.avanzada.entity.Request;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository for Request with custom queries for RF-07 (filter by state, type, priority, responsible).
 */
public interface RequestRepository extends JpaRepository<Request, Long> {

    @Query("SELECT r FROM Request r WHERE (:stateId IS NULL OR r.state.id = :stateId) " +
            "AND (:requestTypeId IS NULL OR r.requestType.id = :requestTypeId) " +
            "AND (:priority IS NULL OR r.priority = :priority) " +
            "AND (:assignedToId IS NULL OR r.assignedTo.id = :assignedToId) " +
            "AND (:requestedById IS NULL OR r.requestedBy.id = :requestedById) " +
            "ORDER BY r.registeredAt DESC")
    List<Request> findByFilters(
            @Param("stateId") Long stateId,
            @Param("requestTypeId") Long requestTypeId,
            @Param("priority") Priority priority,
            @Param("assignedToId") Long assignedToId,
            @Param("requestedById") Long requestedById
    );
}
