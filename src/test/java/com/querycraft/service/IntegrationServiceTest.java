package com.querycraft.service;

import com.querycraft.domain.IntegrationCredential;
import com.querycraft.domain.Project;
import com.querycraft.domain.DatabaseDialect;
import com.querycraft.domain.dto.SaveCredentialRequest;
import com.querycraft.integration.ConfluencePageReader;
import com.querycraft.integration.GitHubRepositoryReader;
import com.querycraft.security.EncryptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.given;

class IntegrationServiceTest {

    private IntegrationService integrationService;
    private EncryptionService encryptionService;

    @Mock private ProjectService projectService;
    @Mock private GitHubRepositoryReader gitHubReader;
    @Mock private ConfluencePageReader confluenceReader;
    @Mock private SchemaIngestionService schemaIngestionService;
    @Mock private VectorStore vectorStore;
    @Mock private TokenTextSplitter textSplitter;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        encryptionService = new EncryptionService("QueryCraftSecretKey32BytesLong!!");
        integrationService = new IntegrationService(
                projectService, encryptionService, gitHubReader, confluenceReader,
                schemaIngestionService, vectorStore, textSplitter
        );
    }

    @Test
    @DisplayName("Should save AES-256 encrypted credential successfully")
    void testSaveEncryptedCredential() {
        Project mockProj = Project.builder().projectId("proj-101").targetDialect(DatabaseDialect.POSTGRESQL).build();
        given(projectService.getProject("proj-101")).willReturn(mockProj);

        SaveCredentialRequest req = SaveCredentialRequest.builder()
                .providerType("GITHUB")
                .accessToken("ghp_secret_token_12345")
                .expiresInSeconds(3600L)
                .build();

        IntegrationCredential result = integrationService.saveCredential("proj-101", req);

        assertNotNull(result);
        assertEquals("GITHUB", result.getProviderType());
        assertNotEquals("ghp_secret_token_12345", result.getEncryptedAccessToken());
        assertEquals("ghp_secret_token_12345", encryptionService.decrypt(result.getEncryptedAccessToken()));
        assertFalse(result.isExpired());
    }
}
