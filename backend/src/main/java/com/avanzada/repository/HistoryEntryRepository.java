package com.avanzada.repository;

import com.avanzada.entity.HistoryEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoryEntryRepository extends JpaRepository<HistoryEntry, Long> {

    List<HistoryEntry> findByRequest_IdOrderByOccurredAtDesc(Long requestId);
}
