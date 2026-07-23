package com.querycraft.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class DdlSchemaParserTest {

    private DdlSchemaParser parser;

    @BeforeEach
    void setUp() {
        parser = new DdlSchemaParser();
    }

    @Test
    @DisplayName("Should detect support for .sql files")
    void testSupports() {
        assertTrue(parser.supports("schema.sql", "application/sql"));
        assertFalse(parser.supports("doc.pdf", "application/pdf"));
    }

    @Test
    @DisplayName("Should parse SQL DDL file and strip single-line comments")
    void testParseDdlFile() {
        String sql = """
                -- This is a comment to strip
                CREATE TABLE products (
                    product_id INT PRIMARY KEY,
                    product_name VARCHAR(100) NOT NULL,
                    price DECIMAL(10,2)
                );
                """;

        ByteArrayInputStream is = new ByteArrayInputStream(sql.getBytes(StandardCharsets.UTF_8));
        String result = parser.parse(is, "schema.sql");

        assertNotNull(result);
        assertFalse(result.contains("-- This is a comment to strip"));
        assertTrue(result.contains("CREATE TABLE products"));
        assertEquals("SQL_DDL", parser.getFileType());
    }
}
