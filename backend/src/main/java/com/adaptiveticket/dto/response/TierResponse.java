package com.adaptiveticket.dto.response;

import lombok.*;
import java.math.BigDecimal;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TierResponse {
    private Long id;
    private String name;
    private BigDecimal basePrice;
    private BigDecimal currentPrice;
    private Integer totalQuantity;
    private Integer sold;
    private Integer remaining;
}
