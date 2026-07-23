package com.querycraft.service;

import com.querycraft.domain.DatabaseDialect;
import com.querycraft.domain.Project;
import com.querycraft.domain.Role;
import com.querycraft.domain.dto.CreateProjectRequest;
import com.querycraft.exception.ProjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class ProjectService {

    private static final Logger log = LoggerFactory.getLogger(ProjectService.class);
    private final Map<String, Project> projects = new ConcurrentHashMap<>();

    public Project createProject(CreateProjectRequest request) {
        String dialectStr = (request.getTargetDialect() != null && !request.getTargetDialect().isBlank())
                ? request.getTargetDialect() : "POSTGRESQL";
        DatabaseDialect dialect = DatabaseDialect.fromString(dialectStr);
        String id = "proj-" + UUID.randomUUID().toString().substring(0, 8);
        Instant now = Instant.now();

        Project project = Project.builder()
                .projectId(id)
                .name(request.getName())
                .targetDialect(dialect)
                .description(request.getDescription())
                .ingestedFiles(new ArrayList<>())
                .assignedUsernames(new HashSet<>())
                .createdAt(now)
                .updatedAt(now)
                .build();

        projects.put(id, project);
        log.info("Created Project [ID: {}, Name: '{}', Dialect: {}]", id, project.getName(), dialect.name());
        return project;
    }

    public Project getProject(String projectId) {
        Project project = projects.get(projectId);
        if (project == null) {
            throw new ProjectNotFoundException(projectId);
        }
        return project;
    }

    public List<Project> listProjects() {
        return new ArrayList<>(projects.values());
    }

    public List<Project> listProjectsForUser(String username, String roleStr) {
        if (username == null || username.isBlank() || Role.ROLE_ADMIN.name().equalsIgnoreCase(roleStr)) {
            return listProjects();
        }

        String sanitizedUser = username.trim().toLowerCase(Locale.ROOT);
        return projects.values().stream()
                .filter(p -> p.getAssignedUsernames().contains(sanitizedUser))
                .collect(Collectors.toList());
    }

    public Project assignUserToProject(String projectId, String username) {
        Project project = getProject(projectId);
        if (username != null && !username.isBlank()) {
            String sanitizedUser = username.trim().toLowerCase(Locale.ROOT);
            project.getAssignedUsernames().add(sanitizedUser);
            project.setUpdatedAt(Instant.now());
            log.info("Assigned user [{}] access to Project [{}]", sanitizedUser, projectId);
        }
        return project;
    }

    public void registerIngestedFile(String projectId, String fileName) {
        Project project = getProject(projectId);
        if (!project.getIngestedFiles().contains(fileName)) {
            project.getIngestedFiles().add(fileName);
            project.setUpdatedAt(Instant.now());
            log.info("Registered file '{}' to project '{}'", fileName, projectId);
        }
    }
}
