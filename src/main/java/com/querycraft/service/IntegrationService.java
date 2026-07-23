package com.querycraft.service;

import com.querycraft.domain.IntegrationCredential;
import com.querycraft.domain.Project;
import com.querycraft.domain.dto.ImportConfluenceRequest;
import com.querycraft.domain.dto.ImportGitHubRequest;
import com.querycraft.domain.dto.IngestionResponse;
import com.querycraft.domain.dto.SaveCredentialRequest;
import com.querycraft.integration.ConfluencePageReader;
import com.querycraft.integration.GitHubRepositoryReader;
import com.querycraft.security.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class IntegrationService {

    private static final Logger log = LoggerFactory.getLogger(IntegrationService.class);

    private final ProjectService projectService;
    private final EncryptionService encryptionService;
    private final GitHubRepositoryReader gitHubReader;
    private final ConfluencePageReader confluenceReader;
    private final SchemaIngestionService schemaIngestionService;
    private final VectorStore vectorStore;
    private final TokenTextSplitter textSplitter;

    // In-Memory store for encrypted credentials keyed by: projectId + ":" + providerType
    private final Map<String, IntegrationCredential> credentialsStore = new ConcurrentHashMap<>();

    @Value("${querycraft.integrations.github.default-token:}")
    private String defaultGitHubToken;

    @Value("${querycraft.integrations.confluence.default-domain:}")
    private String defaultConfluenceDomain;

    @Value("${querycraft.integrations.confluence.default-email:}")
    private String defaultConfluenceEmail;

    @Value("${querycraft.integrations.confluence.default-api-token:}")
    private String defaultConfluenceApiToken;

    public IntegrationService(ProjectService projectService,
                              EncryptionService encryptionService,
                              GitHubRepositoryReader gitHubReader,
                              ConfluencePageReader confluenceReader,
                              SchemaIngestionService schemaIngestionService,
                              VectorStore vectorStore,
                              TokenTextSplitter textSplitter) {
        this.projectService = projectService;
        this.encryptionService = encryptionService;
        this.gitHubReader = gitHubReader;
        this.confluenceReader = confluenceReader;
        this.schemaIngestionService = schemaIngestionService;
        this.vectorStore = vectorStore;
        this.textSplitter = textSplitter;
    }

    public IntegrationCredential saveCredential(String projectId, SaveCredentialRequest request) {
        projectService.getProject(projectId); // Validate project exists
        String key = projectId + ":" + request.getProviderType().toUpperCase(Locale.ROOT);

        Instant expiresAt = request.getExpiresInSeconds() != null ?
                Instant.now().plusSeconds(request.getExpiresInSeconds()) : null;

        IntegrationCredential credential = IntegrationCredential.builder()
                .id(UUID.randomUUID().toString())
                .projectId(projectId)
                .providerType(request.getProviderType().toUpperCase(Locale.ROOT))
                .domain(request.getDomain())
                .email(request.getEmail())
                .encryptedAccessToken(encryptionService.encrypt(request.getAccessToken()))
                .encryptedRefreshToken(encryptionService.encrypt(request.getRefreshToken()))
                .expiresAt(expiresAt)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        credentialsStore.put(key, credential);
        log.info("Saved AES-256 encrypted [{}] credentials for Project [{}]", credential.getProviderType(), projectId);
        return credential;
    }

    public IngestionResponse importFromGitHub(String projectId, ImportGitHubRequest request) {
        Project project = projectService.getProject(projectId);
        String token = getGitHubToken(projectId);

        String rawContent = gitHubReader.fetchFileContent(
                request.getOwner(),
                request.getRepo(),
                request.getFilePath(),
                request.getBranch(),
                token
        );

        String fileName = request.getFilePath().substring(request.getFilePath().lastIndexOf('/') + 1);
        return schemaIngestionService.ingestDocument(projectId, new CustomMultipartFile(fileName, rawContent.getBytes(StandardCharsets.UTF_8)));
    }

    public com.querycraft.domain.dto.BulkRepoIngestionResponse importFromGitHubRepoUrl(String projectId, com.querycraft.domain.dto.ImportGitHubRepoUrlRequest request) {
        Project project = projectService.getProject(projectId);
        GitHubRepositoryReader.RepoInfo repoInfo = gitHubReader.parseRepoUrl(request.getRepoUrl());
        String branch = request.getBranch() != null ? request.getBranch() : repoInfo.branch;
        String token = getGitHubToken(projectId);

        log.info("Starting bulk repository URL crawling for project [{}] on {}/{} (branch: {})",
                projectId, repoInfo.owner, repoInfo.repo, branch);

        List<String> filePaths = gitHubReader.fetchRepositoryTree(repoInfo.owner, repoInfo.repo, branch, token);
        List<IngestionResponse> ingestedFiles = new ArrayList<>();
        int totalChunks = 0;

        for (String filePath : filePaths) {
            try {
                String rawContent = gitHubReader.fetchFileContent(repoInfo.owner, repoInfo.repo, filePath, branch, token);
                String fileName = filePath.contains("/") ? filePath.substring(filePath.lastIndexOf('/') + 1) : filePath;
                IngestionResponse response = schemaIngestionService.ingestDocument(
                        projectId, new CustomMultipartFile(fileName, rawContent.getBytes(StandardCharsets.UTF_8))
                );
                ingestedFiles.add(response);
                totalChunks += response.getChunksIngested();
            } catch (Exception e) {
                log.warn("Skipping file {} during bulk repo import: {}", filePath, e.getMessage());
            }
        }

        return com.querycraft.domain.dto.BulkRepoIngestionResponse.builder()
                .projectId(projectId)
                .repositoryUrl(request.getRepoUrl())
                .filesProcessed(ingestedFiles.size())
                .totalChunksIngested(totalChunks)
                .ingestedFiles(ingestedFiles)
                .message(String.format("Successfully crawled repository %s/%s. Ingested %d files (%d chunks) to GCS and VectorStore.",
                        repoInfo.owner, repoInfo.repo, ingestedFiles.size(), totalChunks))
                .timestamp(Instant.now())
                .build();
    }

    public IngestionResponse importFromConfluence(String projectId, ImportConfluenceRequest request) {
        Project project = projectService.getProject(projectId);
        IntegrationCredential cred = credentialsStore.get(projectId + ":CONFLUENCE");

        String domain = request.getDomain() != null ? request.getDomain() :
                (cred != null && cred.getDomain() != null ? cred.getDomain() : defaultConfluenceDomain);
        String email = (cred != null && cred.getEmail() != null) ? cred.getEmail() : defaultConfluenceEmail;
        String apiToken = (cred != null && cred.getEncryptedAccessToken() != null) ?
                encryptionService.decrypt(cred.getEncryptedAccessToken()) : defaultConfluenceApiToken;

        if (domain.isBlank() || email.isBlank() || apiToken.isBlank()) {
            throw new IllegalArgumentException("Confluence credentials missing. Configure domain, email, and API token via API or application.yml");
        }

        String plainText = confluenceReader.fetchPageContent(domain, request.getPageId(), email, apiToken);

        // Chunk and embed page into VectorStore tagged with metadata
        Document rawDoc = new Document(plainText);
        List<Document> chunks = textSplitter.split(List.of(rawDoc));

        for (Document chunk : chunks) {
            Map<String, Object> metadata = new HashMap<>(chunk.getMetadata());
            metadata.put("projectId", projectId);
            metadata.put("targetDialect", project.getTargetDialect().name());
            metadata.put("fileName", "Confluence_Page_" + request.getPageId());
            metadata.put("fileType", "CONFLUENCE_DOCUMENT");
            metadata.put("confluencePageId", request.getPageId());
            metadata.put("ingestedAt", Instant.now().toString());

            chunk.getMetadata().putAll(metadata);
        }

        vectorStore.add(chunks);
        projectService.registerIngestedFile(projectId, "Confluence_Page_" + request.getPageId());

        return IngestionResponse.builder()
                .projectId(projectId)
                .fileName("Confluence_Page_" + request.getPageId())
                .fileType("CONFLUENCE_DOCUMENT")
                .chunksIngested(chunks.size())
                .message("Successfully fetched, converted, and indexed Confluence Page ID " + request.getPageId())
                .timestamp(Instant.now())
                .build();
    }

    private String getGitHubToken(String projectId) {
        IntegrationCredential cred = credentialsStore.get(projectId + ":GITHUB");
        if (cred != null && cred.getEncryptedAccessToken() != null) {
            if (cred.isExpired()) {
                log.warn("Saved GitHub token for project [{}] is expired!", projectId);
            }
            return encryptionService.decrypt(cred.getEncryptedAccessToken());
        }
        return defaultGitHubToken;
    }

    // Lightweight In-Memory MultipartFile helper for remote string content
    private static class CustomMultipartFile implements org.springframework.web.multipart.MultipartFile {
        private final String name;
        private final byte[] content;

        public CustomMultipartFile(String name, byte[] content) {
            this.name = name;
            this.content = content;
        }

        @Override public String getName() { return name; }
        @Override public String getOriginalFilename() { return name; }
        @Override public String getContentType() { return "text/plain"; }
        @Override public boolean isEmpty() { return content.length == 0; }
        @Override public long getSize() { return content.length; }
        @Override public byte[] getBytes() { return content; }
        @Override public InputStream getInputStream() { return new ByteArrayInputStream(content); }
        @Override public void transferTo(File dest) throws java.io.IOException { new java.io.FileOutputStream(dest).write(content); }
    }
}
