package com.querycraft.controller;

import com.querycraft.domain.Project;
import com.querycraft.domain.dto.CreateProjectRequest;
import com.querycraft.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/projects")
@Tag(name = "Project Management", description = "Endpoints for onboarding projects with target database dialects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @PostMapping
    @Operation(summary = "Create a new project", description = "Onboard a new QueryCraft project specifying target database dialect (POSTGRESQL, MYSQL, ORACLE, SNOWFLAKE)")
    public ResponseEntity<Project> createProject(@Valid @RequestBody CreateProjectRequest request) {
        Project created = projectService.createProject(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/{projectId}")
    @Operation(summary = "Get project details", description = "Fetch project metadata and ingested file list")
    public ResponseEntity<Project> getProject(@PathVariable String projectId) {
        return ResponseEntity.ok(projectService.getProject(projectId));
    }

    @GetMapping
    @Operation(summary = "List all projects", description = "List all active onboarded projects")
    public ResponseEntity<List<Project>> listProjects() {
        return ResponseEntity.ok(projectService.listProjects());
    }
}
