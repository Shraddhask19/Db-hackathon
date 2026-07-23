package com.querycraft.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.querycraft.domain.DatabaseDialect;
import com.querycraft.domain.Project;
import com.querycraft.domain.dto.ChatRequest;
import com.querycraft.domain.dto.ChatResponse;
import com.querycraft.domain.dto.CreateProjectRequest;
import com.querycraft.service.ProjectService;
import com.querycraft.service.TextToSqlService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest({ChatController.class, ProjectController.class})
class ChatControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TextToSqlService textToSqlService;

    @MockBean
    private ProjectService projectService;

    @Test
    @DisplayName("POST /api/v1/projects - Should create project successfully")
    void testCreateProjectApi() throws Exception {
        CreateProjectRequest req = CreateProjectRequest.builder()
                .name("E-Commerce Project")
                .targetDialect("POSTGRESQL")
                .description("Production DB")
                .build();

        Project proj = Project.builder()
                .projectId("proj-1234")
                .name("E-Commerce Project")
                .targetDialect(DatabaseDialect.POSTGRESQL)
                .description("Production DB")
                .ingestedFiles(List.of())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        given(projectService.createProject(any(CreateProjectRequest.class))).willReturn(proj);

        mockMvc.perform(post("/api/v1/projects")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.projectId").value("proj-1234"))
                .andExpect(jsonPath("$.name").value("E-Commerce Project"))
                .andExpect(jsonPath("$.targetDialect").value("POSTGRESQL"));
    }

    @Test
    @DisplayName("POST /api/v1/projects/{id}/chat - Should generate SQL response JSON")
    void testChatApiSuccess() throws Exception {
        ChatRequest chatReq = ChatRequest.builder()
                .conversationId("session-abc")
                .userPrompt("Find top 10 active customers")
                .build();

        ChatResponse mockResp = ChatResponse.builder()
                .conversationId("session-abc")
                .projectId("proj-1234")
                .sql("SELECT id, name FROM customers WHERE active = true LIMIT 10")
                .explanation("Fetches 10 active customers")
                .isValidSelect(true)
                .timestamp(Instant.now())
                .build();

        given(textToSqlService.generateSql(eq("proj-1234"), any(ChatRequest.class))).willReturn(mockResp);

        mockMvc.perform(post("/api/v1/projects/proj-1234/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(chatReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sql").value("SELECT id, name FROM customers WHERE active = true LIMIT 10"))
                .andExpect(jsonPath("$.explanation").value("Fetches 10 active customers"))
                .andExpect(jsonPath("$.isValidSelect").value(true));
    }
}
