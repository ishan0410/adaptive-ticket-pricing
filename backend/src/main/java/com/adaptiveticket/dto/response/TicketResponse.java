package com.adaptiveticket.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TicketResponse {
    private String ticketId;
    private String seat;
    private BigDecimal pricePaid;
}
