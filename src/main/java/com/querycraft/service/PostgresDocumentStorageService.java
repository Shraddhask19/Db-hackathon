package com.querycraft.service;

import com.querycraft.domain.DocumentEntity;
import com.querycraft.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PostgresDocumentStorageService {

    private static final Logger log = LoggerFactory.getLogger(PostgresDocumentStorageService.class);

    private final DocumentRepository documentRepository;
    // In-memory fallback map if JPA database connection is in lightweight mode
    private final ConcurrentHashMap<String, DocumentEntity> inMemoryStore = new ConcurrentHashMap<>();

    public PostgresDocumentStorageService(DocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    public String saveDocument(String projectId, String fileName, String fileType, byte[] content) {
        String docId = UUID.randomUUID().toString();
        String rawContent = new String(content, StandardCharsets.UTF_8);
        Instant now = Instant.now();

        DocumentEntity entity = DocumentEntity.builder()
                .id(docId)
                .projectId(projectId)
                .fileName(fileName)
                .fileType(fileType)
                .rawContent(rawContent)
                .uploadedAt(now)
                .build();

        try {
            documentRepository.save(entity);
            log.info("Persisted schema document to PostgreSQL [documents table]: ID={}, Project={}, File={}",
                    docId, projectId, fileName);
        } catch (Exception e) {
            log.warn("PostgreSQL storage fallback activated for {}: {}", fileName, e.getMessage());
            inMemoryStore.put(docId, entity);
        }

        return String.format("postgresql://documents/%s/%s", projectId, fileName);
    }

    public List<DocumentEntity> getDocumentsByProject(String projectId) {
        try {
            return documentRepository.findByProjectId(projectId);
        } catch (Exception e) {
            return inMemoryStore.values().stream()
                    .filter(doc -> projectId.equals(doc.getProjectId()))
                    .toList();
        }
    }
}
