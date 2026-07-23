package com.querycraft.domain.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SaveCredentialRequest {
    @NotBlank(message = "Provider type must be GITHUB or CONFLUENCE")
    private String providerType;

    private String domain;
    private String email;

    @NotBlank(message = "Access token or Personal Access Token is required")
    private String accessToken;

    private String refreshToken;
    private Long expiresInSeconds;
}
