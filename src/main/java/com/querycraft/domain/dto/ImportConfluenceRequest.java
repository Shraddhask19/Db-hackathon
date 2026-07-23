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
public class ImportConfluenceRequest {
    @NotBlank(message = "Confluence page ID is required")
    private String pageId;

    private String domain; // Optional override, e.g. company.atlassian.net
}
