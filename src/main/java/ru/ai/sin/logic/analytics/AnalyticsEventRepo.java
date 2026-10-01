package ru.ai.sin.logic.analytics;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface AnalyticsEventRepo extends JpaRepository<AnalyticsEventEnt, Long> {

    @Query("SELECT e.path, COUNT(e) FROM AnalyticsEventEnt e WHERE e.occurredAt >= :from AND e.occurredAt < :to GROUP BY e.path ORDER BY COUNT(e) DESC")
    List<Object[]> countByPathBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    @Query("SELECT e.eventType, COUNT(e) FROM AnalyticsEventEnt e WHERE e.occurredAt >= :from AND e.occurredAt < :to GROUP BY e.eventType ORDER BY COUNT(e) DESC")
    List<Object[]> countByEventTypeBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
}
