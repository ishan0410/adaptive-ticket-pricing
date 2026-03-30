package com.adaptiveticket.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CreateEventRequest {

    @NotBlank private String name;
    @NotBlank private String venue;
    @NotBlank private String city;
    private String category;
    @NotNull @Future private LocalDateTime eventDate;
    @NotNull @Min(1) private Integer totalCapacity;
    private String description;
    private String imageUrl;

    @NotEmpty @Valid
    private List<TierRequest> tiers;

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class TierRequest {
        @NotBlank private String name;
        @NotNull @DecimalMin("0.01") private BigDecimal basePrice;
        @NotNull @Min(1) private Integer quantity;
    }
}
