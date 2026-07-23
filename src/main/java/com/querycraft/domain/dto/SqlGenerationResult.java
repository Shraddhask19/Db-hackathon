package com.querycraft.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SqlGenerationResult {

    @JsonProperty(value = "sql", required = true)
    private String sql;

    @JsonProperty(value = "explanation", required = true)
    private String explanation;
}
