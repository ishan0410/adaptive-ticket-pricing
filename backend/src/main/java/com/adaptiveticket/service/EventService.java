package com.adaptiveticket.service;

import com.adaptiveticket.dto.request.CreateEventRequest;
import com.adaptiveticket.dto.response.EventDetailResponse;
import com.adaptiveticket.dto.response.EventListResponse;
import com.adaptiveticket.dto.response.TierResponse;
import com.adaptiveticket.entity.*;
import com.adaptiveticket.exception.ResourceNotFoundException;
import com.adaptiveticket.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final PricingService pricingService;

    @Transactional(readOnly = true)
    public List<EventListResponse> getActiveEvents() {
        List<Event> events = eventRepository.findActiveEventsWithTiers(
                EventStatus.ACTIVE, LocalDateTime.now());

        return events.stream()
                .map(this::toListResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<EventListResponse> getActiveEventsPaginated(String category, Pageable pageable) {
        Page<Event> page;
        if (category != null && !category.isBlank()) {
            page = eventRepository.findByStatusAndCategory(EventStatus.ACTIVE, category, pageable);
        } else {
            page = eventRepository.findByStatus(EventStatus.ACTIVE, pageable);
        }
        return page.map(this::toListResponse);
    }

    @Transactional(readOnly = true)
    public EventDetailResponse getEventDetail(Long id) {
        Event event = eventRepository.findByIdWithTiers(id)
                .orElseThrow(() -> new ResourceNotFoundException("Event", id));

        List<TierResponse> tiers = event.getTiers().stream()
                .map(tier -> TierResponse.builder()
                        .id(tier.getId())
                        .name(tier.getTierName())
                        .basePrice(tier.getBasePrice())
                        .currentPrice(pricingService.calculateCurrentPrice(tier, event))
                        .totalQuantity(tier.getTotalQuantity())
                        .sold(tier.getSold())
                        .remaining(tier.getRemaining())
                        .build())
                .collect(Collectors.toList());

        return EventDetailResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .venue(event.getVenue())
                .city(event.getCity())
                .category(event.getCategory())
                .eventDate(event.getEventDate())
                .description(event.getDescription())
                .imageUrl(event.getImageUrl())
                .totalCapacity(event.getTotalCapacity())
                .totalSold(event.getTotalSold())
                .tiers(tiers)
                .build();
    }

    @Transactional
    public EventDetailResponse createEvent(CreateEventRequest request, User organizer) {
        Event event = Event.builder()
                .organizer(organizer)
                .name(request.getName())
                .venue(request.getVenue())
                .city(request.getCity())
                .category(request.getCategory())
                .eventDate(request.getEventDate())
                .totalCapacity(request.getTotalCapacity())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .build();

        request.getTiers().forEach(tierReq -> {
            TicketTier tier = TicketTier.builder()
                    .event(event)
                    .tierName(tierReq.getName())
                    .basePrice(tierReq.getBasePrice())
                    .currentPrice(tierReq.getBasePrice()) // starts at base
                    .totalQuantity(tierReq.getQuantity())
                    .build();
            event.getTiers().add(tier);
        });

        Event saved = eventRepository.save(event);
        return getEventDetail(saved.getId());
    }

    // ── Private mapping helpers ──

    private EventListResponse toListResponse(Event event) {
        BigDecimal cheapest = event.getTiers().stream()
                .map(tier -> pricingService.calculateCurrentPrice(tier, event))
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        return EventListResponse.builder()
                .id(event.getId())
                .name(event.getName())
                .venue(event.getVenue())
                .city(event.getCity())
                .category(event.getCategory())
                .eventDate(event.getEventDate())
                .imageUrl(event.getImageUrl())
                .totalCapacity(event.getTotalCapacity())
                .totalSold(event.getTotalSold())
                .startingPrice(cheapest)
                .build();
    }
}
