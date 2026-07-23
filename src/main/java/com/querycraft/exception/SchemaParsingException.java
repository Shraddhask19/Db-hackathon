package com.querycraft.exception;

public class SchemaParsingException extends RuntimeException {
    public SchemaParsingException(String message, Throwable cause) {
        super(message, cause);
    }

    public SchemaParsingException(String message) {
        super(message);
    }
}
