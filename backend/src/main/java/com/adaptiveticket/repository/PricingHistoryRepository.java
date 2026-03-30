package com.adaptiveticket.repository;

import com.adaptiveticket.entity.PricingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PricingHistoryRepository extends JpaRepository<PricingHistory, Long> {

    List<PricingHistory> findByTierIdOrderByRecordedAtAsc(Long tierId);

    List<PricingHistory> findByTierEventIdOrderByRecordedAtAsc(Long eventId);
}
