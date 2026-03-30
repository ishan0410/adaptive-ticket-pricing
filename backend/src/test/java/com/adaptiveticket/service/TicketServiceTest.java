package com.adaptiveticket.service;

import com.adaptiveticket.dto.request.PurchaseRequest;
import com.adaptiveticket.dto.response.OrderResponse;
import com.adaptiveticket.entity.*;
import com.adaptiveticket.exception.InsufficientInventoryException;
import com.adaptiveticket.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TicketServiceTest {

    @Mock private EventRepository eventRepository;
    @Mock private TicketTierRepository tierRepository;
    @Mock private OrderRepository orderRepository;
    @Mock private TicketRepository ticketRepository;
    @Mock private PricingService pricingService;

    @InjectMocks
    private TicketService ticketService;

    private User buyer;
    private Event event;
    private TicketTier tier;

    @BeforeEach
    void setUp() {
        buyer = User.builder().id(1L).name("Test Buyer").email("buyer@test.com").role(Role.BUYER).build();

        event = Event.builder()
                .id(1L).name("Test Event").venue("Test Venue").city("Test City")
                .eventDate(LocalDateTime.now().plusDays(30))
                .totalCapacity(100).status(EventStatus.ACTIVE)
                .build();

        tier = TicketTier.builder()
                .id(1L).event(event).tierName("General")
                .basePrice(new BigDecimal("50.00")).currentPrice(new BigDecimal("55.00"))
                .totalQuantity(100).sold(80).version(0L)
                .build();
    }

    @Test
    @DisplayName("Successful purchase creates order and updates inventory")
    void successfulPurchase() {
        PurchaseRequest request = PurchaseRequest.builder()
                .eventId(1L).tierId(1L).quantity(2).build();

        when(tierRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tier));
        when(eventRepository.findByIdWithTiers(1L)).thenReturn(Optional.of(event));
        when(pricingService.calculateCurrentPrice(tier, event)).thenReturn(new BigDecimal("55.00"));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(1L);
            order.setPurchasedAt(LocalDateTime.now());
            return order;
        });

        OrderResponse response = ticketService.purchaseTickets(request, buyer);

        assertThat(response.getQuantity()).isEqualTo(2);
        assertThat(response.getUnitPrice()).isEqualByComparingTo(new BigDecimal("55.00"));
        assertThat(response.getTotal()).isEqualByComparingTo(new BigDecimal("110.00"));
        assertThat(response.getTickets()).hasSize(2);
        assertThat(response.getOrderId()).startsWith("ORD-");

        // Inventory should be updated
        assertThat(tier.getSold()).isEqualTo(82);
        verify(tierRepository).save(tier);
    }

    @Test
    @DisplayName("Purchase fails when insufficient inventory")
    void failsWhenInsufficientInventory() {
        tier.setSold(99); // only 1 remaining
        PurchaseRequest request = PurchaseRequest.builder()
                .eventId(1L).tierId(1L).quantity(5).build();

        when(tierRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tier));
        when(eventRepository.findByIdWithTiers(1L)).thenReturn(Optional.of(event));

        assertThatThrownBy(() -> ticketService.purchaseTickets(request, buyer))
                .isInstanceOf(InsufficientInventoryException.class)
                .hasMessageContaining("1 remaining")
                .hasMessageContaining("5 requested");

        // Inventory should NOT be updated
        assertThat(tier.getSold()).isEqualTo(99);
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("Each ticket gets a unique code and seat label")
    void uniqueTicketCodes() {
        PurchaseRequest request = PurchaseRequest.builder()
                .eventId(1L).tierId(1L).quantity(3).build();

        when(tierRepository.findByIdForUpdate(1L)).thenReturn(Optional.of(tier));
        when(eventRepository.findByIdWithTiers(1L)).thenReturn(Optional.of(event));
        when(pricingService.calculateCurrentPrice(tier, event)).thenReturn(new BigDecimal("55.00"));
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order order = inv.getArgument(0);
            order.setId(1L);
            order.setPurchasedAt(LocalDateTime.now());
            return order;
        });

        OrderResponse response = ticketService.purchaseTickets(request, buyer);

        long uniqueIds = response.getTickets().stream()
                .map(t -> t.getTicketId()).distinct().count();
        assertThat(uniqueIds).isEqualTo(3);
    }
}
