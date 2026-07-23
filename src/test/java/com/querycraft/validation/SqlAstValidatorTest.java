package com.querycraft.validation;

import com.querycraft.exception.InvalidSqlException;
import net.sf.jsqlparser.statement.select.Select;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SqlAstValidatorTest {

    private SqlAstValidator validator;

    @BeforeEach
    void setUp() {
        validator = new SqlAstValidator();
    }

    @Test
    @DisplayName("Should accept valid simple SELECT statement")
    void testValidSimpleSelect() {
        String sql = "SELECT id, name, email FROM users WHERE is_active = true";
        Select result = validator.validateSelectOrThrow(sql);
        assertNotNull(result);
        assertTrue(validator.isValidSelect(sql));
    }

    @Test
    @DisplayName("Should accept complex SELECT query with JOIN, GROUP BY, HAVING, and ORDER BY")
    void testValidComplexSelect() {
        String sql = """
                SELECT u.department_id, COUNT(u.id) AS total_users, AVG(s.salary) AS avg_salary
                FROM users u
                INNER JOIN salaries s ON u.id = s.user_id
                WHERE u.status = 'ACTIVE'
                GROUP BY u.department_id
                HAVING COUNT(u.id) > 5
                ORDER BY avg_salary DESC
                LIMIT 10 OFFSET 0
                """;
        Select result = validator.validateSelectOrThrow(sql);
        assertNotNull(result);
        assertTrue(validator.isValidSelect(sql));
    }

    @Test
    @DisplayName("Should accept SELECT query wrapped in markdown code blocks")
    void testValidSelectMarkdownWrapped() {
        String rawMarkdown = """
                ```sql
                SELECT * FROM orders WHERE total_amount > 100;
                ```
                """;
        Select result = validator.validateSelectOrThrow(rawMarkdown);
        assertNotNull(result);
        assertTrue(validator.isValidSelect(rawMarkdown));
    }

    @Test
    @DisplayName("Should accept CTE WITH clause SELECT query")
    void testValidCteSelect() {
        String sql = """
                WITH RegionalSales AS (
                    SELECT region, SUM(amount) AS total_sales
                    FROM orders
                    GROUP BY region
                )
                SELECT region, total_sales FROM RegionalSales WHERE total_sales > 10000;
                """;
        Select result = validator.validateSelectOrThrow(sql);
        assertNotNull(result);
    }

    @ParameterizedTest
    @DisplayName("Should reject destructive non-SELECT statements (DELETE, UPDATE, INSERT, DROP, ALTER, TRUNCATE, GRANT)")
    @ValueSource(strings = {
            "DELETE FROM users WHERE id = 1",
            "DROP TABLE users",
            "UPDATE users SET is_active = false WHERE status = 'INACTIVE'",
            "INSERT INTO users (id, name) VALUES (1, 'Alice')",
            "ALTER TABLE users ADD COLUMN phone_number VARCHAR(20)",
            "TRUNCATE TABLE audit_logs",
            "GRANT ALL PRIVILEGES ON DATABASE app TO intruder"
    })
    void testRejectNonSelectStatements(String illegalSql) {
        InvalidSqlException ex = assertThrows(InvalidSqlException.class, () -> validator.validateSelectOrThrow(illegalSql));
        assertTrue(ex.getMessage().contains("AST Validation Error"));
        assertFalse(validator.isValidSelect(illegalSql));
    }

    @Test
    @DisplayName("Should reject multi-statement SQL injection attempts")
    void testRejectMultiStatementInjection() {
        String injectionSql = "SELECT id FROM users; DROP TABLE users;";
        InvalidSqlException ex = assertThrows(InvalidSqlException.class, () -> validator.validateSelectOrThrow(injectionSql));
        assertTrue(ex.getMessage().contains("Multiple SQL statements detected"));
        assertFalse(validator.isValidSelect(injectionSql));
    }
}
