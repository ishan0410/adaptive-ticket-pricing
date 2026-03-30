package com.adaptiveticket.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "ticket_tiers", indexes = {
    @Index(name = "idx_tier_event", columnList = "event_id")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketTier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "tier_name", nullable = false, length = 50)
    private String tierName;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(name = "current_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "total_quantity", nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false)
    @Builder.Default
    private Integer sold = 0;

    /**
     * Optimistic locking to handle concurrent ticket purchases.
     * If two requests try to buy the last ticket simultaneously,
     * one will get an OptimisticLockException and retry or fail gracefully.
     */
    @Version
    private Long version;

    public int getRemaining() {
        return totalQuantity - sold;
    }

    public boolean hasAvailability(int quantity) {
        return getRemaining() >= quantity;
    }
}
