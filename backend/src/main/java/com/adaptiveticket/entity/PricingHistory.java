package com.adaptiveticket.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "pricing_history", indexes = {
    @Index(name = "idx_pricing_tier", columnList = "tier_id"),
    @Index(name = "idx_pricing_recorded", columnList = "recordedAt")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PricingHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tier_id", nullable = false)
    private TicketTier tier;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_reason", nullable = false, length = 20)
    private PriceTrigger triggerReason;

    @CreationTimestamp
    @Column(name = "recorded_at", updatable = false)
    private LocalDateTime recordedAt;

    public enum PriceTrigger {
        DEMAND,
        TIME,
        SCARCITY,
        MANUAL
    }
}
