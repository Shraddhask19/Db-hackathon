package com.querycraft.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IntegrationCredential {
    private String id;
    private String projectId;
    private String providerType; // GITHUB, CONFLUENCE
    private String domain;       // e.g. company.atlassian.net
    private String email;        // e.g. user@company.com
    private String encryptedAccessToken;
    private String encryptedRefreshToken;
    private Instant expiresAt;
    private Instant createdAt;
    private Instant updatedAt;

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }
}
