package com.adaptiveticket.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderResponse {
    private String orderId;
    private String event;
    private String tier;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal total;
    private LocalDateTime purchasedAt;
    private List<TicketResponse> tickets;
}
