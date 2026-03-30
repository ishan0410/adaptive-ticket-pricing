package com.adaptiveticket.service;

import com.adaptiveticket.entity.Event;
import com.adaptiveticket.entity.PricingHistory;
import com.adaptiveticket.entity.TicketTier;
import com.adaptiveticket.repository.PricingHistoryRepository;
import com.adaptiveticket.repository.TicketTierRepository;
import com.adaptiveticket.repository.EventRepository;
import com.adaptiveticket.entity.EventStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Core adaptive pricing engine.
 *
 * Computes ticket prices based on three weighted factors:
 *   1. Demand   — price rises as a higher percentage of tickets sell
 *   2. Urgency  — price rises as the event date approaches
 *   3. Scarcity — price spikes when very few tickets remain in a tier
 *
 * Formula:
 *   currentPrice = basePrice × demandFactor × urgencyFactor × scarcityFactor
 *
 * The algorithm runs:
 *   - On every price check (read path, via calculateCurrentPrice)
 *   - Every 15 minutes via a scheduled task that persists updated prices
 *     to the database and records them in pricing_history
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PricingService {

    private final TicketTierRepository tierRepository;
    private final PricingHistoryRepository historyRepository;
    private final EventRepository eventRepository;

    // ── Price calculation (stateless, used on every read) ──

    public BigDecimal calculateCurrentPrice(TicketTier tier, Event event) {
        BigDecimal base = tier.getBasePrice();

        BigDecimal demandFactor = computeDemandFactor(tier);
        BigDecimal urgencyFactor = computeUrgencyFactor(event);
        BigDecimal scarcityFactor = computeScarcityFactor(tier);

        BigDecimal price = base
                .multiply(demandFactor)
                .multiply(urgencyFactor)
                .multiply(scarcityFactor)
                .setScale(2, RoundingMode.HALF_UP);

        log.debug("Pricing tier={} base={} demand={} urgency={} scarcity={} -> {}",
                tier.getTierName(), base, demandFactor, urgencyFactor, scarcityFactor, price);

        return price;
    }

    /**
     * Demand factor: 1.0 + (soldRatio × 0.45)
     * At 0% sold → 1.0x (no markup)
     * At 50% sold → 1.225x
     * At 100% sold → 1.45x
     */
    private BigDecimal computeDemandFactor(TicketTier tier) {
        if (tier.getTotalQuantity() == 0) return BigDecimal.ONE;
        double soldRatio = (double) tier.getSold() / tier.getTotalQuantity();
        double factor = 1.0 + (soldRatio * 0.45);
        return BigDecimal.valueOf(factor);
    }

    /**
     * Urgency factor based on days until event:
     *   < 7 days  → 1.20x
     *   < 30 days → 1.10x
     *   < 90 days → 1.03x
     *   else      → 1.00x
     */
    private BigDecimal computeUrgencyFactor(Event event) {
        long daysUntil = ChronoUnit.DAYS.between(LocalDateTime.now(), event.getEventDate());
        if (daysUntil < 1) daysUntil = 1;

        double factor;
        if (daysUntil < 7) factor = 1.20;
        else if (daysUntil < 30) factor = 1.10;
        else if (daysUntil < 90) factor = 1.03;
        else factor = 1.0;

        return BigDecimal.valueOf(factor);
    }

    /**
     * Scarcity factor based on remaining tickets:
     *   < 10 remaining → 1.15x
     *   < 30 remaining → 1.06x
     *   else           → 1.00x
     */
    private BigDecimal computeScarcityFactor(TicketTier tier) {
        int remaining = tier.getRemaining();
        double factor;
        if (remaining < 10) factor = 1.15;
        else if (remaining < 30) factor = 1.06;
        else factor = 1.0;

        return BigDecimal.valueOf(factor);
    }

    // ── Scheduled recalculation (writes updated prices to DB) ──

    /**
     * Runs every 15 minutes. Recalculates prices for all tiers of active events
     * and persists the new price + a pricing_history record.
     */
    @Scheduled(fixedRate = 900_000) // 15 minutes
    @Transactional
    public void recalculateAllPrices() {
        log.info("Starting scheduled price recalculation");
        List<Event> activeEvents = eventRepository.findActiveEventsWithTiers(
                EventStatus.ACTIVE, LocalDateTime.now());

        int updated = 0;
        for (Event event : activeEvents) {
            for (TicketTier tier : event.getTiers()) {
                BigDecimal newPrice = calculateCurrentPrice(tier, event);

                // Only persist if price actually changed
                if (newPrice.compareTo(tier.getCurrentPrice()) != 0) {
                    tier.setCurrentPrice(newPrice);
                    tierRepository.save(tier);

                    PricingHistory record = PricingHistory.builder()
                            .tier(tier)
                            .price(newPrice)
                            .triggerReason(determineTrigger(tier, event))
                            .build();
                    historyRepository.save(record);
                    updated++;
                }
            }
        }
        log.info("Price recalculation complete. {} tiers updated.", updated);
    }

    private PricingHistory.PriceTrigger determineTrigger(TicketTier tier, Event event) {
        long daysUntil = ChronoUnit.DAYS.between(LocalDateTime.now(), event.getEventDate());
        int remaining = tier.getRemaining();

        if (remaining < 10) return PricingHistory.PriceTrigger.SCARCITY;
        if (daysUntil < 7) return PricingHistory.PriceTrigger.TIME;
        return PricingHistory.PriceTrigger.DEMAND;
    }

    // ── Price history retrieval ──

    public List<PricingHistory> getHistoryForEvent(Long eventId) {
        return historyRepository.findByTierEventIdOrderByRecordedAtAsc(eventId);
    }

    public List<PricingHistory> getHistoryForTier(Long tierId) {
        return historyRepository.findByTierIdOrderByRecordedAtAsc(tierId);
    }
}
