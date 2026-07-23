package com.querycraft.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateProjectRequest {
    @NotBlank(message = "Project name is required")
    private String name;

    @NotNull(message = "Target database dialect is required (POSTGRESQL, MYSQL, ORACLE, SNOWFLAKE)")
    private String targetDialect;

    private String description;
}
