package com.adaptiveticket.controller;

import com.adaptiveticket.dto.request.PurchaseRequest;
import com.adaptiveticket.dto.response.OrderResponse;
import com.adaptiveticket.entity.User;
import com.adaptiveticket.service.TicketService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping("/purchase")
    public ResponseEntity<OrderResponse> purchase(
            @Valid @RequestBody PurchaseRequest request,
            @AuthenticationPrincipal User buyer) {
        OrderResponse response = ticketService.purchaseTickets(request, buyer);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/my-tickets")
    public ResponseEntity<List<OrderResponse>> myTickets(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(ticketService.getUserOrders(user.getId()));
    }
}
