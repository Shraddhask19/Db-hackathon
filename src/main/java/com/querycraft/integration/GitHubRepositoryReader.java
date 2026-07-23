package com.querycraft.integration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class GitHubRepositoryReader {

    private static final Logger log = LoggerFactory.getLogger(GitHubRepositoryReader.class);
    private static final Pattern GITHUB_URL_PATTERN = Pattern.compile(
            "https?://github\\.com/([^/]+)/([^/#?]+)(?:/tree/([^/#?]+))?", Pattern.CASE_INSENSITIVE
    );

    private final RestTemplate restTemplate = new RestTemplate();

    public static class RepoInfo {
        public final String owner;
        public final String repo;
        public final String branch;

        public RepoInfo(String owner, String repo, String branch) {
            this.owner = owner;
            this.repo = repo.endsWith(".git") ? repo.substring(0, repo.length() - 4) : repo;
            this.branch = branch != null && !branch.isBlank() ? branch : "main";
        }
    }

    public RepoInfo parseRepoUrl(String rawUrl) {
        if (rawUrl == null || rawUrl.isBlank()) {
            throw new IllegalArgumentException("GitHub repository URL must not be null or empty");
        }
        Matcher matcher = GITHUB_URL_PATTERN.matcher(rawUrl.trim());
        if (!matcher.find()) {
            throw new IllegalArgumentException("Invalid GitHub Repository URL: " + rawUrl + ". Example: https://github.com/owner/repo");
        }
        return new RepoInfo(matcher.group(1), matcher.group(2), matcher.group(3));
    }

    public List<String> fetchRepositoryTree(String owner, String repo, String branch, String githubToken) {
        log.info("Crawling GitHub repository tree for {}/{} (branch: {})", owner, repo, branch);
        String url = String.format("https://api.github.com/repos/%s/%s/git/trees/%s?recursive=1", owner, repo, branch);

        try {
            HttpHeaders headers = createHeaders(githubToken);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class
            );

            Map body = response.getBody();
            if (body == null || !body.containsKey("tree")) {
                return Collections.emptyList();
            }

            List<Map<String, Object>> tree = (List<Map<String, Object>>) body.get("tree");
            List<String> filePaths = new ArrayList<>();

            for (Map<String, Object> item : tree) {
                String type = (String) item.get("type");
                String path = (String) item.get("path");
                if ("blob".equalsIgnoreCase(type) && isSupportedSchemaFile(path)) {
                    filePaths.add(path);
                }
            }

            log.info("Discovered {} schema/doc files in GitHub repo {}/{}", filePaths.size(), owner, repo);
            return filePaths;

        } catch (Exception e) {
            log.error("Failed to fetch repository tree for {}/{}: {}", owner, repo, e.getMessage());
            return Collections.emptyList();
        }
    }

    public String fetchFileContent(String owner, String repo, String filePath, String branch, String githubToken) {
        log.info("Fetching remote file from GitHub: {}/{} [{}] branch: {}", owner, repo, filePath, branch);
        String ref = (branch != null && !branch.isBlank()) ? branch : "main";
        String url = String.format("https://api.github.com/repos/%s/%s/contents/%s?ref=%s", owner, repo, filePath, ref);

        try {
            HttpHeaders headers = createHeaders(githubToken);
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class
            );

            Map body = response.getBody();
            if (body == null || !body.containsKey("content")) {
                throw new RuntimeException("Empty response body from GitHub API for file: " + filePath);
            }

            String rawBase64 = ((String) body.get("content")).replaceAll("\\s+", "");
            byte[] decodedBytes = Base64.getDecoder().decode(rawBase64);
            return new String(decodedBytes, StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("Failed to read GitHub repository file {}/{}/{}: {}", owner, repo, filePath, e.getMessage());
            throw new RuntimeException("GitHub API Error: " + e.getMessage(), e);
        }
    }

    private HttpHeaders createHeaders(String githubToken) {
        HttpHeaders headers = new HttpHeaders();
        if (githubToken != null && !githubToken.isBlank()) {
            headers.setBearerAuth(githubToken);
        }
        headers.set("Accept", "application/vnd.github.v3+json");
        headers.set("User-Agent", "QueryCraft-AI-Platform");
        return headers;
    }

    private boolean isSupportedSchemaFile(String path) {
        if (path == null) return false;
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".sql") || lower.endsWith(".xml") || lower.endsWith(".pdf") || lower.endsWith(".md");
    }
}
