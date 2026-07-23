package com.querycraft.service;

import com.querycraft.domain.Project;
import com.querycraft.domain.Role;
import com.querycraft.domain.dto.CreateProjectRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectAccessControlTest {

    private ProjectService projectService;

    @BeforeEach
    void setUp() {
        projectService = new ProjectService();
    }

    @Test
    @DisplayName("Admin should see all projects")
    void testAdminSeeAllProjects() {
        Project p1 = projectService.createProject(CreateProjectRequest.builder().name("Banking System").build());
        Project p2 = projectService.createProject(CreateProjectRequest.builder().name("Insurance Core").build());

        List<Project> adminProjects = projectService.listProjectsForUser("admin", Role.ROLE_ADMIN.name());
        assertEquals(2, adminProjects.size());
    }

    @Test
    @DisplayName("Standard User should only see assigned projects")
    void testStandardUserAccess() {
        Project p1 = projectService.createProject(CreateProjectRequest.builder().name("Banking System").build());
        Project p2 = projectService.createProject(CreateProjectRequest.builder().name("Insurance Core").build());

        // Assign user to Banking System
        projectService.assignUserToProject(p1.getProjectId(), "devuser");

        List<Project> userProjects = projectService.listProjectsForUser("devuser", Role.ROLE_USER.name());
        assertEquals(1, userProjects.size());
        assertEquals("Banking System", userProjects.get(0).getName());
    }
}
