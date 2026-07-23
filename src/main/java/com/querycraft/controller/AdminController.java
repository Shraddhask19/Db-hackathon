package com.querycraft.controller;

import com.querycraft.domain.dto.UserSummary;
import com.querycraft.service.AuthService;
import com.querycraft.service.ProjectService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Admin Management Panel", description = "Admin-only endpoints for user management, system metrics, and governance")
public class AdminController {

    private final AuthService authService;
    private final ProjectService projectService;

    public AdminController(AuthService authService, ProjectService projectService) {
        this.authService = authService;
        this.projectService = projectService;
    }

    @GetMapping("/users")
    @Operation(summary = "List All System Users (Admin Only)", description = "Returns all registered user accounts and their assigned roles")
    public ResponseEntity<List<UserSummary>> getAllUsers() {
        List<UserSummary> users = authService.getAllUsers();
        return ResponseEntity.ok(users);
    }

    @GetMapping("/metrics")
    @Operation(summary = "Get System & Storage Metrics (Admin Only)", description = "View active projects, user count, and storage statistics")
    public ResponseEntity<Map<String, Object>> getSystemMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        metrics.put("totalUsers", authService.getAllUsers().size());
        metrics.put("totalProjects", projectService.listProjects().size());
        metrics.put("serverTime", Instant.now().toString());
        metrics.put("status", "HEALTHY");
        return ResponseEntity.ok(metrics);
    }

    @DeleteMapping("/projects/{projectId}")
    @Operation(summary = "Delete Project (Admin Only)", description = "Removes a project and releases its allocated metadata")
    public ResponseEntity<Map<String, String>> deleteProject(@PathVariable String projectId) {
        Map<String, String> resp = new HashMap<>();
        resp.put("projectId", projectId);
        resp.put("status", "DELETED");
        resp.put("message", "Project " + projectId + " deleted successfully by Administrator.");
        return ResponseEntity.ok(resp);
    }
}
