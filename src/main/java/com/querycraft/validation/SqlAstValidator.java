package com.querycraft.validation;

import com.querycraft.exception.InvalidSqlException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.Select;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Strict AST Validator powered by JSqlParser.
 * Enforces that only valid SQL SELECT statements are allowed for execution and response.
 * Blocks any attempt to issue DDL (DROP, CREATE, ALTER), DML (DELETE, UPDATE, INSERT),
 * control commands, or multi-statement SQL injections.
 */
@Component
public class SqlAstValidator {

    private static final Logger log = LoggerFactory.getLogger(SqlAstValidator.class);
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("^```(?:sql)?\\s*(.*?)\\s*```$", Pattern.DOTALL | Pattern.CASE_INSENSITIVE);

    /**
     * Sanitizes raw LLM output, extracting SQL from markdown blocks if present.
     */
    public String sanitizeSql(String rawSql) {
        if (rawSql == null) {
            return "";
        }
        String cleaned = rawSql.trim();
        var matcher = CODE_BLOCK_PATTERN.matcher(cleaned);
        if (matcher.matches()) {
            cleaned = matcher.group(1).trim();
        }
        return cleaned;
    }

    /**
     * Validates that the provided SQL string parses successfully and is strictly an instance of net.sf.jsqlparser.statement.select.Select.
     *
     * @param rawSql SQL string to validate
     * @return The parsed Select statement AST object
     * @throws InvalidSqlException if SQL is invalid, multi-statement, or not a SELECT statement
     */
    public Select validateSelectOrThrow(String rawSql) {
        String sanitizedSql = sanitizeSql(rawSql);

        if (sanitizedSql.isBlank()) {
            throw new InvalidSqlException("SQL query string is empty or blank", rawSql);
        }

        try {
            // First check statement count to prevent SQL injection chaining (e.g. SELECT 1; DROP TABLE users;)
            Statements statements = CCJSqlParserUtil.parseStatements(sanitizedSql);
            if (statements.getStatements().size() > 1) {
                log.warn("Rejected multi-statement SQL execution: {}", sanitizedSql);
                throw new InvalidSqlException(
                        "AST Validation Error: Multiple SQL statements detected. Only a single SELECT query is permitted.",
                        sanitizedSql
                );
            }

            Statement statement = CCJSqlParserUtil.parse(sanitizedSql);

            if (!(statement instanceof Select selectStatement)) {
                String statementTypeName = statement.getClass().getSimpleName();
                log.warn("Rejected non-SELECT statement type [{}]: {}", statementTypeName, sanitizedSql);
                throw new InvalidSqlException(
                        String.format("AST Validation Error: Rejected [%s] statement. Only SELECT queries are permitted.", statementTypeName),
                        sanitizedSql
                );
            }

            log.debug("SQL AST Validation Passed successfully for SELECT statement: {}", sanitizedSql);
            return selectStatement;

        } catch (InvalidSqlException e) {
            throw e;
        } catch (Exception e) {
            log.error("JSqlParser failed to parse SQL: {} - Error: {}", sanitizedSql, e.getMessage());
            throw new InvalidSqlException(
                    "AST Validation Error: Failed to parse SQL syntax: " + e.getMessage(),
                    sanitizedSql,
                    e
            );
        }
    }

    /**
     * Returns true if the SQL is a valid SELECT statement, false otherwise.
     */
    public boolean isValidSelect(String rawSql) {
        try {
            validateSelectOrThrow(rawSql);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
