package com.avanzada.repository;

import com.avanzada.entity.HistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface HistoryEntryRepository extends JpaRepository<HistoryEntry, Long> {

    List<HistoryEntry> findByRequest_IdOrderByOccurredAtDesc(Long requestId);

    Optional<HistoryEntry> findFirstByRequest_IdAndActionOrderByOccurredAtDesc(Long requestId, String action);
}
