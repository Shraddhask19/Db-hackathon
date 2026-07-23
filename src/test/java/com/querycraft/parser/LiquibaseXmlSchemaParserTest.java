package com.querycraft.parser;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class LiquibaseXmlSchemaParserTest {

    private LiquibaseXmlSchemaParser parser;

    @BeforeEach
    void setUp() {
        parser = new LiquibaseXmlSchemaParser();
    }

    @Test
    @DisplayName("Should detect support for .xml files")
    void testSupports() {
        assertTrue(parser.supports("db.changelog.xml", "application/xml"));
        assertTrue(parser.supports("schema.xml", "text/xml"));
        assertFalse(parser.supports("schema.sql", "application/sql"));
    }

    @Test
    @DisplayName("Should parse Liquibase XML createTable and addColumn tags into DDL")
    void testParseLiquibaseXml() {
        String xmlContent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <databaseChangeLog xmlns="http://www.liquibase.org/xml/ns/dbchangelog">
                    <changeSet id="1" author="architect">
                        <createTable tableName="customers" remarks="Customer Master Table">
                            <column name="id" type="BIGINT">
                                <constraints primaryKey="true" nullable="false"/>
                            </column>
                            <column name="email" type="VARCHAR(255)">
                                <constraints nullable="false" unique="true"/>
                            </column>
                        </createTable>
                    </changeSet>
                </databaseChangeLog>
                """;

        ByteArrayInputStream is = new ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8));
        String parsedDdl = parser.parse(is, "db.changelog.xml");

        assertNotNull(parsedDdl);
        assertTrue(parsedDdl.contains("CREATE TABLE customers"));
        assertTrue(parsedDdl.contains("id BIGINT PRIMARY KEY NOT NULL"));
        assertTrue(parsedDdl.contains("email VARCHAR(255) NOT NULL UNIQUE"));
        assertEquals("LIQUIBASE_XML", parser.getFileType());
    }
}
