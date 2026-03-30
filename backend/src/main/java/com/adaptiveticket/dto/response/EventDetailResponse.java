package com.adaptiveticket.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EventDetailResponse {
    private Long id;
    private String name;
    private String venue;
    private String city;
    private String category;
    private LocalDateTime eventDate;
    private String description;
    private String imageUrl;
    private Integer totalCapacity;
    private Integer totalSold;
    private List<TierResponse> tiers;
}
