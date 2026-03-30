package com.adaptiveticket.controller;

import com.adaptiveticket.entity.PricingHistory;
import com.adaptiveticket.service.PricingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/events")
@RequiredArgsConstructor
public class PricingController {

    private final PricingService pricingService;

    @GetMapping("/{eventId}/pricing-history")
    public ResponseEntity<List<PricingHistory>> getPricingHistory(@PathVariable Long eventId) {
        return ResponseEntity.ok(pricingService.getHistoryForEvent(eventId));
    }
}
