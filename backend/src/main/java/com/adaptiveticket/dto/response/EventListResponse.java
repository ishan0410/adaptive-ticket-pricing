package com.adaptiveticket.dto.response;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventListResponse {
    private Long id;
    private String name;
    private String venue;
    private String city;
    private String category;
    private LocalDateTime eventDate;
    private String imageUrl;
    private Integer totalCapacity;
    private Integer totalSold;
    private BigDecimal startingPrice;
}
