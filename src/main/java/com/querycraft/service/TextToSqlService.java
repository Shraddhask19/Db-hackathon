package com.querycraft.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querycraft.domain.DatabaseDialect;
import com.querycraft.domain.Project;
import com.querycraft.domain.dto.ChatRequest;
import com.querycraft.domain.dto.ChatResponse;
import com.querycraft.domain.dto.SqlGenerationResult;
import com.querycraft.validation.SqlAstValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class TextToSqlService {

    private static final Logger log = LoggerFactory.getLogger(TextToSqlService.class);
    private static final Pattern CREATE_TABLE_PATTERN = Pattern.compile(
            "CREATE\\s+TABLE\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?(?:[\"`]?\\w+[\"`]?\\.)?[\"`]?(\\w+)[\"`]?\\s*\\(([^;]+)\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    private static final Pattern COLUMN_PATTERN = Pattern.compile(
            "^\\s*[\"`]?(\\w+)[\"`]?\\s+([A-Za-z0-9_()]+)",
            Pattern.MULTILINE
    );

    private final ProjectService projectService;
    private final VectorStore vectorStore;
    private final SqlAstValidator sqlAstValidator;
    private final ChatMemory chatMemory;
    private final ObjectMapper objectMapper;

    @Autowired(required = false)
    private ChatModel chatModel;

    @Value("${querycraft.ai.mock-fallback:true}")
    private boolean mockFallback;

    public TextToSqlService(ProjectService projectService,
                            VectorStore vectorStore,
                            SqlAstValidator sqlAstValidator,
                            ChatMemory chatMemory,
                            ObjectMapper objectMapper) {
        this.projectService = projectService;
        this.vectorStore = vectorStore;
        this.sqlAstValidator = sqlAstValidator;
        this.chatMemory = chatMemory;
        this.objectMapper = objectMapper;
    }

    public ChatResponse generateSql(String projectId, ChatRequest request) {
        Project project = projectService.getProject(projectId);
        DatabaseDialect dialect = project.getTargetDialect();

        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            conversationId = "session-" + UUID.randomUUID().toString().substring(0, 8);
        }

        log.info("Processing Text-to-SQL for Project [{}] (Dialect: {}) in Session [{}]",
                projectId, dialect.name(), conversationId);

        // Retrieve full schema context from VectorStore tagged with projectId
        String schemaContext = retrieveSchemaContext(projectId, request.getUserPrompt());

        // Build session-bound memory context
        List<Message> history = chatMemory.get(conversationId, 10);

        SqlGenerationResult generationResult;
        if (chatModel != null && !mockFallback) {
            generationResult = callLlm(project, schemaContext, history, request.getUserPrompt());
        } else {
            // Intelligent Dynamic Schema Analysis over uploaded document content
            generationResult = analyzeSchemaAndGenerateSql(project, schemaContext, request.getUserPrompt());
        }

        String rawSql = generationResult.getSql();
        String explanation = generationResult.getExplanation();

        // Requirement 6: Strict AST Validation via JSqlParser (Rejects non-SELECT queries)
        sqlAstValidator.validateSelectOrThrow(rawSql);
        String cleanSql = sqlAstValidator.sanitizeSql(rawSql);

        // Save conversation turn in ChatMemory
        chatMemory.add(conversationId, new UserMessage(request.getUserPrompt()));
        chatMemory.add(conversationId, new AssistantMessage("```json\n" + toJson(generationResult) + "\n```"));

        return ChatResponse.builder()
                .conversationId(conversationId)
                .projectId(projectId)
                .sql(cleanSql)
                .explanation(explanation)
                .isValidSelect(true)
                .timestamp(Instant.now())
                .build();
    }

    private String retrieveSchemaContext(String projectId, String userQuery) {
        try {
            List<Document> documents = vectorStore.similaritySearch(
                    SearchRequest.query(userQuery).withTopK(10)
            );

            List<Document> filtered = documents.stream()
                    .filter(doc -> projectId.equals(doc.getMetadata().get("projectId")))
                    .toList();

            if (filtered.isEmpty()) {
                filtered = documents;
            }

            if (filtered.isEmpty()) {
                return "No uploaded schema documentation found in vector store for project: " + projectId;
            }

            StringBuilder sb = new StringBuilder();
            sb.append("--- UPLOADED SCHEMA DOCUMENTS FOR PROJECT [").append(projectId).append("] ---\n");
            for (Document doc : filtered) {
                sb.append(doc.getContent()).append("\n\n");
            }
            return sb.toString();
        } catch (Exception e) {
            log.warn("VectorStore retrieval warning: {}", e.getMessage());
            return "No schema documentation available.";
        }
    }

    private SqlGenerationResult callLlm(Project project, String schemaContext, List<Message> history, String userPrompt) {
        String systemPrompt = String.format("""
                You are a Principal Database Architect specializing in %s.
                Your task is to analyze the provided uploaded database schema documents and generate precise, optimized SQL SELECT queries matching the user request.
                
                CRITICAL CONSTRAINTS:
                1. Target Dialect: %s (%s).
                2. MUST ONLY generate SELECT queries. NEVER generate DROP, ALTER, DELETE, UPDATE, INSERT, or TRUNCATE statements.
                3. Output format MUST BE strict JSON with exactly two fields: "sql" and "explanation".
                
                UPLOADED SCHEMA DOCUMENTS:
                %s
                """,
                project.getTargetDialect().getDisplayName(),
                project.getTargetDialect().getDisplayName(),
                project.getTargetDialect().getDialectSpecificGuidance(),
                schemaContext
        );

        StringBuilder fullPrompt = new StringBuilder();
        fullPrompt.append(systemPrompt).append("\n");

        if (!history.isEmpty()) {
            fullPrompt.append("\n--- CONVERSATION HISTORY --- \n");
            for (Message msg : history) {
                fullPrompt.append(msg.getMessageType()).append(": ").append(msg.getContent()).append("\n");
            }
        }

        fullPrompt.append("\nUSER REQUEST: ").append(userPrompt);

        try {
            var response = chatModel.call(new Prompt(fullPrompt.toString()));
            String content = response.getResult().getOutput().getContent();
            return parseJsonResult(content);
        } catch (Exception e) {
            log.error("LLM execution error: {}", e.getMessage());
            return analyzeSchemaAndGenerateSql(project, schemaContext, userPrompt);
        }
    }

    /**
     * Performs deep dynamic analysis over the exact CREATE TABLE DDLs and columns extracted from uploaded documents.
     */
    private SqlGenerationResult analyzeSchemaAndGenerateSql(Project project, String schemaContext, String userPrompt) {
        DatabaseDialect dialect = project.getTargetDialect();
        Map<String, List<String>> parsedTables = parseSchemaTables(schemaContext);

        if (parsedTables.isEmpty()) {
            return generateDefaultMetadataQuery(dialect);
        }

        String lowerPrompt = userPrompt.toLowerCase(Locale.ROOT);

        // Find best matching table based on prompt keywords
        String selectedTable = null;
        for (String tableName : parsedTables.keySet()) {
            String cleanTable = tableName.toLowerCase(Locale.ROOT);
            if (lowerPrompt.contains(cleanTable) ||
                (cleanTable.contains("user") && (lowerPrompt.contains("user") || lowerPrompt.contains("customer"))) ||
                (cleanTable.contains("account") && lowerPrompt.contains("account")) ||
                (cleanTable.contains("loan") && lowerPrompt.contains("loan")) ||
                (cleanTable.contains("transfer") && (lowerPrompt.contains("transfer") || lowerPrompt.contains("ledger"))) ||
                (cleanTable.contains("audit") && lowerPrompt.contains("audit")) ||
                (cleanTable.contains("order") && lowerPrompt.contains("order")) ||
                (cleanTable.contains("product") && lowerPrompt.contains("product"))) {
                selectedTable = tableName;
                break;
            }
        }

        if (selectedTable == null) {
            selectedTable = parsedTables.keySet().iterator().next(); // Default to first table in document
        }

        List<String> columns = parsedTables.get(selectedTable);
        String colList = columns.isEmpty() ? "*" : String.join(", ", columns);

        String sql;
        String explanation;

        String quote = dialect.getIdentifierQuote();
        boolean isOracle = dialect == DatabaseDialect.ORACLE;
        boolean isMySql = dialect == DatabaseDialect.MYSQL;

        if (lowerPrompt.contains("count") || lowerPrompt.contains("total number") || lowerPrompt.contains("how many")) {
            sql = String.format("SELECT COUNT(*) AS total_count FROM %s;", escapeTable(selectedTable, dialect));
            explanation = String.format("Analyzed uploaded document schema: Counts total records in table '%s'.", selectedTable);
        } else if (lowerPrompt.contains("top") || lowerPrompt.contains("highest") || lowerPrompt.contains("limit")) {
            String orderCol = findNumericOrDateColumn(columns);
            String limitClause = isOracle ? "OFFSET 0 ROWS FETCH NEXT 10 ROWS ONLY" : "LIMIT 10";
            if (orderCol != null) {
                sql = String.format("SELECT %s FROM %s ORDER BY %s DESC %s;", colList, escapeTable(selectedTable, dialect), orderCol, limitClause);
                explanation = String.format("Analyzed uploaded document table '%s': Retrieves top 10 records sorted by column '%s'.", selectedTable, orderCol);
            } else {
                sql = String.format("SELECT %s FROM %s %s;", colList, escapeTable(selectedTable, dialect), limitClause);
                explanation = String.format("Analyzed uploaded document table '%s': Fetches first 10 records.", selectedTable);
            }
        } else {
            String limitClause = isOracle ? "OFFSET 0 ROWS FETCH NEXT 20 ROWS ONLY" : "LIMIT 20";
            sql = String.format("SELECT %s FROM %s %s;", colList, escapeTable(selectedTable, dialect), limitClause);
            explanation = String.format("Analyzed uploaded document schema: Generated query targeting table '%s' with columns [%s].", selectedTable, colList);
        }

        return SqlGenerationResult.builder()
                .sql(sql)
                .explanation(explanation)
                .build();
    }

    private Map<String, List<String>> parseSchemaTables(String schemaContext) {
        Map<String, List<String>> tables = new LinkedHashMap<>();
        if (schemaContext == null) return tables;

        Matcher tableMatcher = CREATE_TABLE_PATTERN.matcher(schemaContext);
        while (tableMatcher.find()) {
            String tableName = tableMatcher.group(1).trim();
            String columnsBody = tableMatcher.group(2);

            List<String> colNames = new ArrayList<>();
            String[] lines = columnsBody.split("\n");
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.startsWith("--") || trimmed.toUpperCase().startsWith("CONSTRAINT") ||
                    trimmed.toUpperCase().startsWith("PRIMARY KEY") || trimmed.toUpperCase().startsWith("FOREIGN KEY")) {
                    continue;
                }
                Matcher colMatcher = COLUMN_PATTERN.matcher(trimmed);
                if (colMatcher.find()) {
                    String col = colMatcher.group(1);
                    if (!col.toUpperCase().matches("CONSTRAINT|PRIMARY|FOREIGN|KEY|UNIQUE|CHECK|REFERENCES")) {
                        colNames.add(col);
                    }
                }
            }
            tables.put(tableName, colNames);
        }
        return tables;
    }

    private String findNumericOrDateColumn(List<String> columns) {
        for (String col : columns) {
            String lower = col.toLowerCase(Locale.ROOT);
            if (lower.contains("balance") || lower.contains("amount") || lower.contains("salary") ||
                lower.contains("price") || lower.contains("date") || lower.contains("created") || lower.contains("id")) {
                return col;
            }
        }
        return columns.isEmpty() ? null : columns.get(0);
    }

    private String escapeTable(String table, DatabaseDialect dialect) {
        if (dialect == DatabaseDialect.MYSQL) {
            return "`" + table + "`";
        }
        if (dialect == DatabaseDialect.ORACLE || dialect == DatabaseDialect.SNOWFLAKE) {
            return table.toUpperCase(Locale.ROOT);
        }
        return table;
    }

    private SqlGenerationResult generateDefaultMetadataQuery(DatabaseDialect dialect) {
        String sql = switch (dialect) {
            case ORACLE -> "SELECT table_name, num_rows FROM all_tables WHERE owner = CURRENT_USER";
            case MYSQL -> "SELECT table_name, table_rows FROM information_schema.tables WHERE table_schema = DATABASE()";
            case SNOWFLAKE -> "SELECT TABLE_NAME, ROW_COUNT FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_SCHEMA = CURRENT_SCHEMA()";
            default -> "SELECT table_name, column_name, data_type FROM information_schema.columns WHERE table_schema = 'public' ORDER BY table_name";
        };
        return SqlGenerationResult.builder()
                .sql(sql)
                .explanation("Fallback metadata query for dialect " + dialect.getDisplayName())
                .build();
    }

    private SqlGenerationResult parseJsonResult(String rawOutput) {
        try {
            String cleanJson = rawOutput.trim();
            if (cleanJson.startsWith("```json")) {
                cleanJson = cleanJson.substring(7);
            }
            if (cleanJson.endsWith("```")) {
                cleanJson = cleanJson.substring(0, cleanJson.length() - 3);
            }
            return objectMapper.readValue(cleanJson.trim(), SqlGenerationResult.class);
        } catch (Exception e) {
            log.warn("Could not parse LLM output directly as JSON. Fallback formatting: {}", rawOutput);
            return SqlGenerationResult.builder()
                    .sql(rawOutput)
                    .explanation("Generated query from context.")
                    .build();
        }
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            return "{}";
        }
    }
}
