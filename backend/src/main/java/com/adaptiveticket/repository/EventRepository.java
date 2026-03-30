package com.adaptiveticket.repository;

import com.adaptiveticket.entity.Event;
import com.adaptiveticket.entity.EventStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {

    Page<Event> findByStatus(EventStatus status, Pageable pageable);

    Page<Event> findByStatusAndCategory(EventStatus status, String category, Pageable pageable);

    /**
     * Fetch event with tiers eagerly to avoid N+1 query on listing page.
     * This was a key optimization — the original query fired a separate
     * SELECT for tiers on each event row.
     */
    @Query("SELECT DISTINCT e FROM Event e LEFT JOIN FETCH e.tiers WHERE e.status = :status AND e.eventDate > :now")
    List<Event> findActiveEventsWithTiers(@Param("status") EventStatus status, @Param("now") LocalDateTime now);

    @Query("SELECT DISTINCT e FROM Event e LEFT JOIN FETCH e.tiers WHERE e.id = :id")
    Optional<Event> findByIdWithTiers(@Param("id") Long id);

    List<Event> findByOrganizerId(Long organizerId);
}
