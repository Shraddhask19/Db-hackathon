package com.querycraft.service;

import com.querycraft.domain.DatabaseDialect;
import com.querycraft.domain.Project;
import com.querycraft.domain.dto.CreateProjectRequest;
import com.querycraft.exception.ProjectNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

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

    public void registerIngestedFile(String projectId, String fileName) {
        Project project = getProject(projectId);
        if (!project.getIngestedFiles().contains(fileName)) {
            project.getIngestedFiles().add(fileName);
            project.setUpdatedAt(Instant.now());
            log.info("Registered file '{}' to project '{}'", fileName, projectId);
        }
    }
}
