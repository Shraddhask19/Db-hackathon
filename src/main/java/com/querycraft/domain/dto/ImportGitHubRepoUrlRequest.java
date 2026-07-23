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
public class ImportGitHubRepoUrlRequest {

    @NotBlank(message = "GitHub repository URL is required (e.g. https://github.com/owner/repo)")
    private String repoUrl;

    private String branch; // Optional override
}
