package com.adaptiveticket.service;

import com.adaptiveticket.entity.Event;
import com.adaptiveticket.entity.EventStatus;
import com.adaptiveticket.entity.TicketTier;
import com.adaptiveticket.repository.EventRepository;
import com.adaptiveticket.repository.PricingHistoryRepository;
import com.adaptiveticket.repository.TicketTierRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class PricingServiceTest {

    @Mock private TicketTierRepository tierRepository;
    @Mock private PricingHistoryRepository historyRepository;
    @Mock private EventRepository eventRepository;

    @InjectMocks
    private PricingService pricingService;

    private Event event;
    private TicketTier tier;

    @BeforeEach
    void setUp() {
        event = Event.builder()
                .id(1L)
                .name("Test Event")
                .eventDate(LocalDateTime.now().plusDays(60))
                .totalCapacity(500)
                .status(EventStatus.ACTIVE)
                .build();

        tier = TicketTier.builder()
                .id(1L)
                .event(event)
                .tierName("General Admission")
                .basePrice(new BigDecimal("50.00"))
                .currentPrice(new BigDecimal("50.00"))
                .totalQuantity(300)
                .sold(0)
                .build();
    }

    @Nested
    @DisplayName("Demand Factor")
    class DemandFactor {

        @Test
        @DisplayName("Price equals base when no tickets sold")
        void priceAtBaseWhenNoSales() {
            tier.setSold(0);
            BigDecimal price = pricingService.calculateCurrentPrice(tier, event);

            // demand = 1.0, urgency = 1.03 (60 days out), scarcity = 1.0
            // 50.00 * 1.0 * 1.03 * 1.0 = 51.50
            assertThat(price).isEqualByComparingTo(new BigDecimal("51.50"));
        }

        @Test
        @DisplayName("Price increases at 50% sold")
        void priceIncreasesAtHalfSold() {
            tier.setSold(150); // 50% of 300
            BigDecimal price = pricingService.calculateCurrentPrice(tier, event);

            // demand = 1 + (0.5 * 0.45) = 1.225
            // 50.00 * 1.225 * 1.03 * 1.0 = 63.09
            assertThat(price).isGreaterThan(new BigDecimal("60.00"));
        }

        @Test
        @DisplayName("Price spikes near sellout")
        void priceSpikesNearSellout() {
            tier.setSold(290); // 96.7% sold, 10 remaining
            BigDecimal price = pricingService.calculateCurrentPrice(tier, event);

            // demand = 1 + (0.967 * 0.45) = 1.435
            // scarcity < 30 remaining = 1.06
            // 50.00 * 1.435 * 1.03 * 1.06 = 78.42 approx
            assertThat(price).isGreaterThan(new BigDecimal("75.00"));
        }
    }

    @Nested
    @DisplayName("Urgency Factor")
    class UrgencyFactor {

        @Test
        @DisplayName("Price gets 20% urgency boost within 7 days")
        void urgencyBoostWithin7Days() {
            event.setEventDate(LocalDateTime.now().plusDays(3));
            tier.setSold(0);
            BigDecimal price = pricingService.calculateCurrentPrice(tier, event);

            // demand = 1.0, urgency = 1.20, scarcity = 1.0
            // 50.00 * 1.0 * 1.20 * 1.0 = 60.00
            assertThat(price).isEqualByComparingTo(new BigDecimal("60.00"));
        }

        @Test
        @DisplayName("Price gets 10% urgency boost within 30 days")
        void urgencyBoostWithin30Days() {
            event.setEventDate(LocalDateTime.now().plusDays(15));
            tier.setSold(0);
            BigDecimal price = pricingService.calculateCurrentPrice(tier, event);

            // demand = 1.0, urgency = 1.10, scarcity = 1.0
            // 50.00 * 1.0 * 1.10 * 1.0 = 55.00
            assertThat(price).isEqualByComparingTo(new BigDecimal("55.00"));
        }

        @Test
        @DisplayName("No urgency boost for events over 90 days away")
        void noUrgencyFor90PlusDays() {
            event.setEventDate(LocalDateTime.now().plusDays(120));
            tier.setSold(0);
            BigDecimal price = pricingService.calculateCurrentPrice(tier, event);

            // All factors at 1.0
            assertThat(price).isEqualByComparingTo(new BigDecimal("50.00"));
        }
    }

    @Nested
    @DisplayName("Scarcity Factor")
    class ScarcityFactor {

        @Test
        @DisplayName("15% scarcity spike when fewer than 10 remaining")
        void scarcitySpikeUnder10() {
            event.setEventDate(LocalDateTime.now().plusDays(120)); // no urgency
            tier.setSold(295); // 5 remaining
            BigDecimal price = pricingService.calculateCurrentPrice(tier, event);

            // demand = 1 + (0.983 * 0.45) = 1.4425
            // urgency = 1.0
            // scarcity = 1.15
            // 50.00 * 1.4425 * 1.0 * 1.15 = 82.94
            assertThat(price).isGreaterThan(new BigDecimal("80.00"));
        }

        @Test
        @DisplayName("6% scarcity boost when fewer than 30 remaining")
        void scarcityBoostUnder30() {
            event.setEventDate(LocalDateTime.now().plusDays(120));
            tier.setSold(280); // 20 remaining
            BigDecimal price = pricingService.calculateCurrentPrice(tier, event);

            // scarcity = 1.06
            assertThat(price).isGreaterThan(new BigDecimal("50.00"));
        }
    }

    @Test
    @DisplayName("All factors compound together for high-demand near-sellout event")
    void allFactorsCompound() {
        // Near sellout + imminent event = maximum price pressure
        event.setEventDate(LocalDateTime.now().plusDays(2));
        tier.setSold(295); // 5 remaining

        BigDecimal price = pricingService.calculateCurrentPrice(tier, event);

        // demand = 1.4425, urgency = 1.20, scarcity = 1.15
        // 50.00 * 1.4425 * 1.20 * 1.15 ≈ 99.53
        assertThat(price).isGreaterThan(new BigDecimal("95.00"));
        // Should never exceed 2x base in reasonable conditions
        assertThat(price).isLessThan(new BigDecimal("110.00"));
    }
}
