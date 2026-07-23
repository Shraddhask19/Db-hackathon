package com.querycraft.domain;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Project {
    private String projectId;
    private String name;
    private DatabaseDialect targetDialect;
    private String description;
    @Builder.Default
    private List<String> ingestedFiles = new ArrayList<>();
    private Instant createdAt;
    private Instant updatedAt;
}
