package com.querycraft.domain.dto;

import com.querycraft.domain.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String userId;
    private String username;
    private String email;
    private Role role;
    private String message;
    private Instant timestamp;
}
