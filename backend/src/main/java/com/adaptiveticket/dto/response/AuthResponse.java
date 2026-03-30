package com.adaptiveticket.dto.response;

import com.adaptiveticket.entity.Role;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class AuthResponse {
    private Long id;
    private String name;
    private String email;
    private Role role;
    private String token;
}
