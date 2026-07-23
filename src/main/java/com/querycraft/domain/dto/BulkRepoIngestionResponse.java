package com.querycraft.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkRepoIngestionResponse {
    private String projectId;
    private String repositoryUrl;
    private int filesProcessed;
    private int totalChunksIngested;
    private List<IngestionResponse> ingestedFiles;
    private String message;
    private Instant timestamp;
}
