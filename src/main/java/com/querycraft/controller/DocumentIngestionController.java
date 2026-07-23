package com.querycraft.controller;

import com.querycraft.domain.dto.IngestionResponse;
import com.querycraft.service.SchemaIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/documents")
@Tag(name = "Schema Document Ingestion", description = "Endpoints to upload SQL DDL, Liquibase XML, and PDF schema files")
public class DocumentIngestionController {

    private final SchemaIngestionService schemaIngestionService;

    public DocumentIngestionController(SchemaIngestionService schemaIngestionService) {
        this.schemaIngestionService = schemaIngestionService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload schema file", description = "Upload a SQL DDL (.sql), Liquibase XML (.xml), or PDF (.pdf) file to extract, embed, and store in VectorStore tagged with projectId metadata")
    public ResponseEntity<IngestionResponse> uploadDocument(
            @PathVariable String projectId,
            @RequestParam("file") MultipartFile file) {
        
        IngestionResponse response = schemaIngestionService.ingestDocument(projectId, file);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
