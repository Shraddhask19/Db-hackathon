package com.querycraft.service;

import com.querycraft.domain.DocumentEntity;
import com.querycraft.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

class PostgresDocumentStorageServiceTest {

    private PostgresDocumentStorageService storageService;

    @Mock
    private DocumentRepository documentRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        storageService = new PostgresDocumentStorageService(documentRepository);
    }

    @Test
    @DisplayName("Should store document content in PostgreSQL documents table")
    void testSaveDocument() {
        byte[] content = "CREATE TABLE users (id INT PRIMARY KEY);".getBytes(StandardCharsets.UTF_8);

        String resultUri = storageService.saveDocument("proj-101", "schema.sql", "SQL_DDL", content);

        assertNotNull(resultUri);
        assertTrue(resultUri.startsWith("postgresql://documents/proj-101/schema.sql"));
    }
}
