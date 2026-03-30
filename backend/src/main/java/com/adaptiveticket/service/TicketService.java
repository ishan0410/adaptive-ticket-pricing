package com.adaptiveticket.service;

import com.adaptiveticket.dto.request.PurchaseRequest;
import com.adaptiveticket.dto.response.OrderResponse;
import com.adaptiveticket.dto.response.TicketResponse;
import com.adaptiveticket.entity.*;
import com.adaptiveticket.exception.InsufficientInventoryException;
import com.adaptiveticket.exception.ResourceNotFoundException;
import com.adaptiveticket.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TicketService {

    private final EventRepository eventRepository;
    private final TicketTierRepository tierRepository;
    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;
    private final PricingService pricingService;

    /**
     * Purchase tickets for a given event tier.
     *
     * Uses PESSIMISTIC_WRITE lock on the tier row to prevent overselling.
     * The flow:
     *   1. Lock the tier row (SELECT ... FOR UPDATE)
     *   2. Check remaining inventory
     *   3. Calculate current adaptive price
     *   4. Create order + ticket records
     *   5. Increment sold count on the tier
     *   6. Commit (releases the lock)
     *
     * If two requests hit simultaneously, one will wait for the lock.
     * If inventory runs out while waiting, an InsufficientInventoryException is thrown.
     */
    @Transactional
    public OrderResponse purchaseTickets(PurchaseRequest request, User buyer) {
        // 1. Lock the tier row
        TicketTier tier = tierRepository.findByIdForUpdate(request.getTierId())
                .orElseThrow(() -> new ResourceNotFoundException("TicketTier", request.getTierId()));

        Event event = eventRepository.findByIdWithTiers(tier.getEvent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Event", tier.getEvent().getId()));

        // 2. Check availability
        if (!tier.hasAvailability(request.getQuantity())) {
            throw new InsufficientInventoryException(tier.getTierName(), tier.getRemaining(), request.getQuantity());
        }

        // 3. Get current price at time of purchase
        BigDecimal unitPrice = pricingService.calculateCurrentPrice(tier, event);
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(request.getQuantity()))
                .setScale(2, RoundingMode.HALF_UP);

        // 4. Create order
        String orderNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        Order order = Order.builder()
                .user(buyer)
                .orderNumber(orderNumber)
                .totalAmount(total)
                .build();

        // 5. Create individual ticket records
        List<Ticket> tickets = new ArrayList<>();
        for (int i = 0; i < request.getQuantity(); i++) {
            String ticketCode = "TKT-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            String seatLabel = tier.getTierName().substring(0, 2).toUpperCase()
                    + "-" + (tier.getSold() + i + 1);

            Ticket ticket = Ticket.builder()
                    .order(order)
                    .tier(tier)
                    .ticketCode(ticketCode)
                    .pricePaid(unitPrice)
                    .seatLabel(seatLabel)
                    .build();
            tickets.add(ticket);
        }
        order.setTickets(tickets);

        // 6. Update inventory
        tier.setSold(tier.getSold() + request.getQuantity());
        tierRepository.save(tier);

        Order savedOrder = orderRepository.save(order);

        log.info("Purchase complete: order={} event={} tier={} qty={} total={}",
                orderNumber, event.getName(), tier.getTierName(), request.getQuantity(), total);

        return toOrderResponse(savedOrder, event.getName(), tier.getTierName(), unitPrice);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(Long userId) {
        return orderRepository.findByUserIdWithTickets(userId).stream()
                .map(order -> {
                    Ticket firstTicket = order.getTickets().isEmpty() ? null : order.getTickets().get(0);
                    String eventName = firstTicket != null ? firstTicket.getTier().getEvent().getName() : "Unknown";
                    String tierName = firstTicket != null ? firstTicket.getTier().getTierName() : "Unknown";
                    BigDecimal unitPrice = firstTicket != null ? firstTicket.getPricePaid() : BigDecimal.ZERO;
                    return toOrderResponse(order, eventName, tierName, unitPrice);
                })
                .collect(Collectors.toList());
    }

    private OrderResponse toOrderResponse(Order order, String eventName, String tierName, BigDecimal unitPrice) {
        List<TicketResponse> ticketResponses = order.getTickets().stream()
                .map(t -> TicketResponse.builder()
                        .ticketId(t.getTicketCode())
                        .seat(t.getSeatLabel())
                        .pricePaid(t.getPricePaid())
                        .build())
                .collect(Collectors.toList());

        return OrderResponse.builder()
                .orderId(order.getOrderNumber())
                .event(eventName)
                .tier(tierName)
                .quantity(order.getTickets().size())
                .unitPrice(unitPrice)
                .total(order.getTotalAmount())
                .purchasedAt(order.getPurchasedAt())
                .tickets(ticketResponses)
                .build();
    }
}
