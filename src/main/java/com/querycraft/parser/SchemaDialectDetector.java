package com.querycraft.parser;

import com.querycraft.domain.DatabaseDialect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class SchemaDialectDetector {

    private static final Logger log = LoggerFactory.getLogger(SchemaDialectDetector.class);

    public DatabaseDialect detectDialect(String fileContent, String fileName) {
        if (fileContent == null || fileContent.isBlank()) {
            return DatabaseDialect.ANSI_SQL;
        }

        String contentLower = fileContent.toLowerCase(Locale.ROOT);

        // 1. MySQL DDL Markers: backticks, ENGINE=, AUTO_INCREMENT, DATETIME
        if (contentLower.contains("`") || contentLower.contains("engine=") ||
            contentLower.contains("auto_increment") || contentLower.contains("tinyint")) {
            log.info("Auto-Detected database dialect [MYSQL] for file: {}", fileName);
            return DatabaseDialect.MYSQL;
        }

        // 2. Oracle DDL Markers: VARCHAR2, NUMBER(, SYSDATE, DUAL, NVARCHAR2, CLOB
        if (contentLower.contains("varchar2") || contentLower.contains("sysdate") ||
            contentLower.contains("from dual") || contentLower.contains("nvarchar2") || contentLower.contains("number(")) {
            log.info("Auto-Detected database dialect [ORACLE] for file: {}", fileName);
            return DatabaseDialect.ORACLE;
        }

        // 3. PostgreSQL DDL Markers: SERIAL, BIGSERIAL, TIMESTAMPTZ, JSONB, ILIKE, ::
        if (contentLower.contains("serial") || contentLower.contains("bigserial") ||
            contentLower.contains("timestamptz") || contentLower.contains("jsonb") ||
            contentLower.contains("boolean") || contentLower.contains("text")) {
            log.info("Auto-Detected database dialect [POSTGRESQL] for file: {}", fileName);
            return DatabaseDialect.POSTGRESQL;
        }

        // 4. Snowflake DDL Markers: VARIANT, TIMESTAMP_NTZ, COPY INTO
        if (contentLower.contains("variant") || contentLower.contains("timestamp_ntz") ||
            contentLower.contains("copy into")) {
            log.info("Auto-Detected database dialect [SNOWFLAKE] for file: {}", fileName);
            return DatabaseDialect.SNOWFLAKE;
        }

        log.info("Defaulting database dialect to [POSTGRESQL / ANSI_SQL] for file: {}", fileName);
        return DatabaseDialect.POSTGRESQL;
    }
}
