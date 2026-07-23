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
public class ImportGitHubRequest {
    @NotBlank(message = "GitHub repository owner is required")
    private String owner;

    @NotBlank(message = "GitHub repository name is required")
    private String repo;

    @NotBlank(message = "File path in repository is required (e.g. src/main/resources/schema.sql)")
    private String filePath;

    private String branch; // Default "main"
}
