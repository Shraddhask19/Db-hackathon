package com.querycraft.controller;

import com.querycraft.domain.dto.ChatRequest;
import com.querycraft.domain.dto.ChatResponse;
import com.querycraft.service.TextToSqlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/projects/{projectId}/chat")
@Tag(name = "Text-to-SQL Agent Chat", description = "Endpoints for multi-turn AI Text-to-SQL prompt interactions with JSqlParser AST safety validation")
public class ChatController {

    private final TextToSqlService textToSqlService;

    public ChatController(TextToSqlService textToSqlService) {
        this.textToSqlService = textToSqlService;
    }

    @PostMapping
    @Operation(summary = "Chat with project agent", description = "Submit natural language prompt to generate dialect-specific SQL. Session memory preserved via conversationId.")
    public ResponseEntity<ChatResponse> chat(
            @PathVariable String projectId,
            @Valid @RequestBody ChatRequest chatRequest) {

        ChatResponse response = textToSqlService.generateSql(projectId, chatRequest);
        return ResponseEntity.ok(response);
    }
}
