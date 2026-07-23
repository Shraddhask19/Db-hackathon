package com.querycraft.parser;

import com.querycraft.exception.SchemaParsingException;

import java.io.InputStream;

public interface SchemaParser {
    
    /**
     * Determines whether this parser supports the given file based on name or mime type.
     */
    boolean supports(String fileName, String contentType);

    /**
     * Parses the schema input stream and returns formatted schema definition text for vector embedding.
     */
    String parse(InputStream inputStream, String fileName) throws SchemaParsingException;

    /**
     * Returns descriptive file type identifier (e.g. "SQL_DDL", "LIQUIBASE_XML", "PDF_DOCUMENT").
     */
    String getFileType();
}
