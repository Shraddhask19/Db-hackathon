package com.querycraft.controller;

import com.querycraft.domain.IntegrationCredential;
import com.querycraft.domain.dto.ImportConfluenceRequest;
import com.querycraft.domain.dto.ImportGitHubRequest;
import com.querycraft.domain.dto.IngestionResponse;
import com.querycraft.domain.dto.SaveCredentialRequest;
import com.querycraft.service.IntegrationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/integrations")
@Tag(name = "External Integrations", description = "Endpoints for saving credentials and importing schemas from GitHub Repos and Confluence Pages")
public class IntegrationController {

    private final IntegrationService integrationService;

    public IntegrationController(IntegrationService integrationService) {
        this.integrationService = integrationService;
    }

    @PostMapping("/credentials")
    @Operation(summary = "Save Encrypted Credentials", description = "Stores AES-256 encrypted access tokens / API keys for GitHub or Confluence for a project")
    public ResponseEntity<IntegrationCredential> saveCredential(
            @PathVariable String projectId,
            @Valid @RequestBody SaveCredentialRequest request) {

        IntegrationCredential credential = integrationService.saveCredential(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(credential);
    }

    @PostMapping("/github/import")
    @Operation(summary = "Import GitHub Schema File", description = "Fetch and ingest a SQL DDL or Liquibase XML schema file directly from a GitHub repository")
    public ResponseEntity<IngestionResponse> importGitHub(
            @PathVariable String projectId,
            @Valid @RequestBody ImportGitHubRequest request) {

        IngestionResponse response = integrationService.importFromGitHub(projectId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/github/import-repo")
    @Operation(summary = "Import Full GitHub Repository by URL", description = "Crawls an entire GitHub repository URL (e.g. https://github.com/owner/repo), uploads all schema files (.sql, .xml, .pdf, .md) to GCS, and embeds them into VectorStore")
    public ResponseEntity<com.querycraft.domain.dto.BulkRepoIngestionResponse> importGitHubRepoUrl(
            @PathVariable String projectId,
            @Valid @RequestBody com.querycraft.domain.dto.ImportGitHubRepoUrlRequest request) {

        com.querycraft.domain.dto.BulkRepoIngestionResponse response = integrationService.importFromGitHubRepoUrl(projectId, request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/confluence/import")
    @Operation(summary = "Import Confluence Page", description = "Fetch, clean, and ingest data dictionary documentation directly from a Confluence page")
    public ResponseEntity<IngestionResponse> importConfluence(
            @PathVariable String projectId,
            @Valid @RequestBody ImportConfluenceRequest request) {

        IngestionResponse response = integrationService.importFromConfluence(projectId, request);
        return ResponseEntity.ok(response);
    }
}
