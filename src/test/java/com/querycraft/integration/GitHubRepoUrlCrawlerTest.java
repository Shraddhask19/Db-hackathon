package com.querycraft.integration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitHubRepoUrlCrawlerTest {

    private GitHubRepositoryReader reader;

    @BeforeEach
    void setUp() {
        reader = new GitHubRepositoryReader();
    }

    @Test
    @DisplayName("Should parse standard GitHub repo URL")
    void testParseStandardRepoUrl() {
        GitHubRepositoryReader.RepoInfo info = reader.parseRepoUrl("https://github.com/my-org/my-banking-repo");
        assertEquals("my-org", info.owner);
        assertEquals("my-banking-repo", info.repo);
        assertEquals("main", info.branch);
    }

    @Test
    @DisplayName("Should parse GitHub repo URL with branch /tree/develop")
    void testParseRepoUrlWithBranch() {
        GitHubRepositoryReader.RepoInfo info = reader.parseRepoUrl("https://github.com/company/service/tree/develop");
        assertEquals("company", info.owner);
        assertEquals("service", info.repo);
        assertEquals("develop", info.branch);
    }

    @Test
    @DisplayName("Should strip .git extension from repo URL")
    void testParseRepoUrlWithGitExtension() {
        GitHubRepositoryReader.RepoInfo info = reader.parseRepoUrl("https://github.com/company/service.git");
        assertEquals("company", info.owner);
        assertEquals("service", info.repo);
    }
}
