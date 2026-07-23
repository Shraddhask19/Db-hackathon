package com.querycraft.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class GcsDocumentStorageServiceTest {

    private GcsDocumentStorageService gcsStorageService;

    @BeforeEach
    void setUp() {
        gcsStorageService = new GcsDocumentStorageService();
    }

    @Test
    @DisplayName("Should generate proper GCS URI format gs://<bucket>/<projectId>/<fileName>")
    void testStoreDocumentGcsUriFormatting() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "schema.sql",
                "text/plain",
                "CREATE TABLE test (id INT);".getBytes(StandardCharsets.UTF_8)
        );

        String gcsUri = gcsStorageService.storeDocument("proj-1234", file);

        assertNotNull(gcsUri);
        assertTrue(gcsUri.startsWith("gs://querycraft-schema-documents/proj-1234/schema.sql"));
    }
}
