package com.querycraft.domain;

import lombok.Getter;

@Getter
public enum DatabaseDialect {
    POSTGRESQL(
            "PostgreSQL",
            "\"identifier\"",
            "LIMIT n OFFSET m",
            "Use standard PostgreSQL types (VARCHAR, INTEGER, TIMESTAMP, JSONB, BOOLEAN). Use double quotes for identifier escaping if required."
    ),
    MYSQL(
            "MySQL",
            "`identifier`",
            "LIMIT n OFFSET m",
            "Use MySQL types (VARCHAR, INT, DATETIME, JSON). Use backticks for identifier escaping if required."
    ),
    ORACLE(
            "Oracle SQL",
            "\"IDENTIFIER\"",
            "OFFSET m ROWS FETCH NEXT n ROWS ONLY",
            "Use Oracle types (VARCHAR2, NUMBER, DATE, TIMESTAMP, CLOB). Use uppercase identifiers and standard ANSI/Oracle SQL syntax."
    ),
    SNOWFLAKE(
            "Snowflake SQL",
            "\"IDENTIFIER\"",
            "LIMIT n OFFSET m",
            "Use Snowflake types (VARCHAR, NUMBER, TIMESTAMP_NTZ, VARIANT). Use uppercase table and column names where applicable."
    );

    private final String displayName;
    private final String identifierQuote;
    private final String paginationSyntax;
    private final String dialectSpecificGuidance;

    DatabaseDialect(String displayName, String identifierQuote, String paginationSyntax, String dialectSpecificGuidance) {
        this.displayName = displayName;
        this.identifierQuote = identifierQuote;
        this.paginationSyntax = paginationSyntax;
        this.dialectSpecificGuidance = dialectSpecificGuidance;
    }

    public static DatabaseDialect fromString(String dialectStr) {
        if (dialectStr == null || dialectStr.isBlank()) {
            throw new IllegalArgumentException("Database dialect must not be null or empty");
        }
        for (DatabaseDialect dialect : values()) {
            if (dialect.name().equalsIgnoreCase(dialectStr.trim()) ||
                dialect.getDisplayName().equalsIgnoreCase(dialectStr.trim())) {
                return dialect;
            }
        }
        throw new IllegalArgumentException("Unsupported database dialect: " + dialectStr + ". Supported dialects: POSTGRESQL, MYSQL, ORACLE, SNOWFLAKE");
    }
}
