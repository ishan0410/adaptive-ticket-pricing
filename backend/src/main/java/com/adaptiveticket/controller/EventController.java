package com.adaptiveticket.controller;

import com.adaptiveticket.dto.request.CreateEventRequest;
import com.adaptiveticket.dto.response.EventDetailResponse;
import com.adaptiveticket.dto.response.EventListResponse;
import com.adaptiveticket.entity.User;
import com.adaptiveticket.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @GetMapping
    public ResponseEntity<Page<EventListResponse>> listEvents(
            @RequestParam(required = false) String category,
            @PageableDefault(size = 10, sort = "eventDate") Pageable pageable) {
        return ResponseEntity.ok(eventService.getActiveEventsPaginated(category, pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<EventDetailResponse> getEvent(@PathVariable Long id) {
        return ResponseEntity.ok(eventService.getEventDetail(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ORGANIZER') or hasRole('ADMIN')")
    public ResponseEntity<EventDetailResponse> createEvent(
            @Valid @RequestBody CreateEventRequest request,
            @AuthenticationPrincipal User organizer) {
        EventDetailResponse response = eventService.createEvent(request, organizer);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
