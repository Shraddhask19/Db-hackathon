package com.querycraft.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IngestionResponse {
    private String projectId;
    private String fileName;
    private String fileType;
    private String gcsUri;
    private int chunksIngested;
    private String message;
    private Instant timestamp;
}
