package com.querycraft.service;

import com.querycraft.domain.DatabaseDialect;
import com.querycraft.domain.Project;
import com.querycraft.domain.dto.IngestionResponse;
import com.querycraft.parser.SchemaDialectDetector;
import com.querycraft.parser.SchemaParser;
import com.querycraft.parser.SchemaParserFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SchemaIngestionService {

    private static final Logger log = LoggerFactory.getLogger(SchemaIngestionService.class);

    private final ProjectService projectService;
    private final SchemaParserFactory parserFactory;
    private final PostgresDocumentStorageService postgresStorageService;
    private final SchemaDialectDetector dialectDetector;
    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;

    @Value("${querycraft.vectorstore.storage-path:./data/vector_store.json}")
    private String vectorStorePath;

    public SchemaIngestionService(ProjectService projectService,
                                  SchemaParserFactory parserFactory,
                                  PostgresDocumentStorageService postgresStorageService,
                                  SchemaDialectDetector dialectDetector,
                                  VectorStore vectorStore,
                                  TokenTextSplitter textSplitter) {
        this.projectService = projectService;
        this.parserFactory = parserFactory;
        this.postgresStorageService = postgresStorageService;
        this.dialectDetector = dialectDetector;
        this.vectorStore = vectorStore;
        this.textSplitter = textSplitter;
    }

    public IngestionResponse ingestDocument(String projectId, MultipartFile file) {
        Project project = projectService.getProject(projectId);
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename() : "schema_file";
        String contentType = file.getContentType();

        log.info("Processing schema ingestion & PostgreSQL storage for project [{}] file: '{}' ({})", projectId, fileName, contentType);

        SchemaParser parser = parserFactory.getParser(fileName, contentType);

        try (InputStream inputStream = file.getInputStream()) {
            byte[] fileBytes = file.getBytes();

            // Step 1: Upload raw document directly to PostgreSQL (documents table)
            String docUri = postgresStorageService.saveDocument(projectId, fileName, parser.getFileType(), fileBytes);

            // Step 2: Parse schema text
            String extractedSchemaText = parser.parse(new java.io.ByteArrayInputStream(fileBytes), fileName);

            // Step 3: Auto-Detect database dialect directly from uploaded schema file
            DatabaseDialect detectedDialect = dialectDetector.detectDialect(extractedSchemaText, fileName);
            project.setTargetDialect(detectedDialect);
            log.info("Dynamically set target dialect for Project [{}] to [{}] from uploaded file: '{}'", projectId, detectedDialect, fileName);

            // Step 4: Chunk extracted text using TokenTextSplitter
            Document rawDoc = new Document(extractedSchemaText);
            List<Document> chunks = textSplitter.split(List.of(rawDoc));

            // Step 5: Tag each document chunk with metadata: projectId, targetDialect, fileName, fileType, docUri
            for (Document chunk : chunks) {
                Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
                metadata.put("projectId", projectId);
                metadata.put("targetDialect", project.getTargetDialect().name());
                metadata.put("fileName", fileName);
                metadata.put("fileType", parser.getFileType());
                metadata.put("docUri", docUri);
                metadata.put("gcsUri", docUri); // Backward compatible URI field
                metadata.put("ingestedAt", Instant.now().toString());
                
                chunk.getMetadata().putAll(metadata);
            }

            // Step 6: Write metadata-enriched document chunks into VectorStore
            vectorStore.add(chunks);

            // Save SimpleVectorStore state
            if (vectorStore instanceof SimpleVectorStore simpleVectorStore) {
                try {
                    File fileStore = new File(vectorStorePath);
                    File parent = fileStore.getParentFile();
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs();
                    }
                    simpleVectorStore.save(fileStore);
                    log.info("Persisted updated vector store to {}", fileStore.getAbsolutePath());
                } catch (Exception e) {
                    log.warn("Failed to persist vector store file: {}", e.getMessage());
                }
            }

            projectService.registerIngestedFile(projectId, fileName);

            return IngestionResponse.builder()
                    .projectId(projectId)
                    .fileName(fileName)
                    .fileType(parser.getFileType())
                    .gcsUri(docUri)
                    .chunksIngested(chunks.size())
                    .message("Successfully stored in PostgreSQL (" + docUri + "), detected dialect [" + detectedDialect + "], embedded, and indexed " + chunks.size() + " schema chunks tagged with metadata 'projectId'=" + projectId)
                    .timestamp(Instant.now())
                    .build();

        } catch (Exception e) {
            log.error("Failed to ingest document '{}' for project [{}]: {}", fileName, projectId, e.getMessage());
            throw new RuntimeException("Ingestion failed for file '" + fileName + "': " + e.getMessage(), e);
        }
    }
}
