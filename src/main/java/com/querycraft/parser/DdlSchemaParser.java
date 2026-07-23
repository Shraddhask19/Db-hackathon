package com.querycraft.parser;

import com.querycraft.exception.SchemaParsingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;

@Component
public class DdlSchemaParser implements SchemaParser {

    private static final Logger log = LoggerFactory.getLogger(DdlSchemaParser.class);

    @Override
    public boolean supports(String fileName, String contentType) {
        if (fileName != null && fileName.toLowerCase(Locale.ROOT).endsWith(".sql")) {
            return true;
        }
        return contentType != null && (contentType.equalsIgnoreCase("text/plain") ||
                                       contentType.equalsIgnoreCase("application/sql") ||
                                       contentType.equalsIgnoreCase("text/x-sql"));
    }

    @Override
    public String parse(InputStream inputStream, String fileName) throws SchemaParsingException {
        log.info("Parsing SQL DDL file: {}", fileName);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                // Strip single-line SQL comments starting with --
                if (trimmed.startsWith("--")) {
                    continue;
                }
                sb.append(line).append("\n");
            }
            String content = sb.toString().trim();
            if (content.isEmpty()) {
                throw new SchemaParsingException("SQL DDL file is empty: " + fileName);
            }
            return "--- SQL DDL Schema Source: " + fileName + " ---\n" + content;
        } catch (Exception e) {
            log.error("Failed to parse SQL DDL file {}: {}", fileName, e.getMessage());
            throw new SchemaParsingException("Failed to read SQL DDL content from file: " + fileName, e);
        }
    }

    @Override
    public String getFileType() {
        return "SQL_DDL";
    }
}
