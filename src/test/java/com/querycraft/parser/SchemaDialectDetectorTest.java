package com.querycraft.parser;

import com.querycraft.domain.DatabaseDialect;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SchemaDialectDetectorTest {

    private SchemaDialectDetector detector;

    @BeforeEach
    void setUp() {
        detector = new SchemaDialectDetector();
    }

    @Test
    @DisplayName("Should detect MySQL dialect from backticks and AUTO_INCREMENT")
    void testDetectMySQL() {
        String mysqlDdl = """
                CREATE TABLE `users` (
                    `id` INT AUTO_INCREMENT PRIMARY KEY,
                    `name` VARCHAR(100) NOT NULL,
                    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB;
                """;
        DatabaseDialect dialect = detector.detectDialect(mysqlDdl, "schema.sql");
        assertEquals(DatabaseDialect.MYSQL, dialect);
    }

    @Test
    @DisplayName("Should detect Oracle dialect from VARCHAR2 and SYSDATE")
    void testDetectOracle() {
        String oracleDdl = """
                CREATE TABLE employees (
                    emp_id NUMBER(10) PRIMARY KEY,
                    first_name VARCHAR2(50),
                    hire_date DATE DEFAULT SYSDATE
                );
                """;
        DatabaseDialect dialect = detector.detectDialect(oracleDdl, "oracle_schema.sql");
        assertEquals(DatabaseDialect.ORACLE, dialect);
    }

    @Test
    @DisplayName("Should detect PostgreSQL dialect from SERIAL and TIMESTAMPTZ")
    void testDetectPostgreSQL() {
        String pgDdl = """
                CREATE TABLE accounts (
                    id SERIAL PRIMARY KEY,
                    balance NUMERIC(15, 2),
                    created_at TIMESTAMPTZ DEFAULT NOW()
                );
                """;
        DatabaseDialect dialect = detector.detectDialect(pgDdl, "pg_schema.sql");
        assertEquals(DatabaseDialect.POSTGRESQL, dialect);
    }

    @Test
    @DisplayName("Should detect Snowflake dialect from VARIANT")
    void testDetectSnowflake() {
        String sfDdl = """
                CREATE TABLE analytics_events (
                    event_id VARCHAR,
                    payload VARIANT,
                    event_time TIMESTAMP_NTZ
                );
                """;
        DatabaseDialect dialect = detector.detectDialect(sfDdl, "sf_schema.sql");
        assertEquals(DatabaseDialect.SNOWFLAKE, dialect);
    }
}
