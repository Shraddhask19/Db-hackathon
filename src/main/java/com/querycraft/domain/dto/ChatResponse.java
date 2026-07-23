package com.querycraft.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String conversationId;
    private String projectId;

    @JsonProperty("sql")
    private String sql;

    @JsonProperty("explanation")
    private String explanation;

    @JsonProperty("isValidSelect")
    private boolean isValidSelect;
    private Instant timestamp;
}
