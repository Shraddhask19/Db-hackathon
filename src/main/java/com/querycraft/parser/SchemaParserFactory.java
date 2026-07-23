package com.querycraft.parser;

import com.querycraft.exception.UnsupportedFileException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SchemaParserFactory {

    private static final Logger log = LoggerFactory.getLogger(SchemaParserFactory.class);
    private final List<SchemaParser> parsers;

    public SchemaParserFactory(List<SchemaParser> parsers) {
        this.parsers = parsers;
    }

    public SchemaParser getParser(String fileName, String contentType) {
        for (SchemaParser parser : parsers) {
            if (parser.supports(fileName, contentType)) {
                log.debug("Selected parser [{}] for file: {} ({})", parser.getFileType(), fileName, contentType);
                return parser;
            }
        }
        throw new UnsupportedFileException(
                "Unsupported file format for '" + fileName + "'. QueryCraft supports SQL DDL (.sql), Liquibase XML (.xml), and PDF (.pdf) files."
        );
    }
}
