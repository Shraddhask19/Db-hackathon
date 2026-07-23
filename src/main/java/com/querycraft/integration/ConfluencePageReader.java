package com.querycraft.integration;

import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class ConfluencePageReader {

    private static final Logger log = LoggerFactory.getLogger(ConfluencePageReader.class);
    private final RestTemplate restTemplate = new RestTemplate();

    public String fetchPageContent(String domain, String pageId, String email, String apiToken) {
        log.info("Fetching Confluence page ID [{}] from domain [{}]", pageId, domain);
        String cleanDomain = domain.replace("https://", "").replace("http://", "").replaceAll("/+$", "");
        String url = String.format("https://%s/wiki/api/v2/pages/%s?body-format=storage", cleanDomain, pageId);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(email, apiToken);
            headers.set("Accept", "application/json");

            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class
            );

            Map body = response.getBody();
            if (body == null || !body.containsKey("body")) {
                throw new RuntimeException("Empty response body from Confluence API for page: " + pageId);
            }

            Map bodyMap = (Map) body.get("body");
            Map storageMap = (Map) bodyMap.get("storage");
            String rawHtml = (String) storageMap.get("value");

            // Strip HTML formatting and convert to clean plain text using Jsoup
            String plainText = Jsoup.parse(rawHtml).text();
            String title = body.containsKey("title") ? (String) body.get("title") : "Confluence Page " + pageId;

            return "--- Confluence Page Title: " + title + " (ID: " + pageId + ") ---\n" + plainText;

        } catch (Exception e) {
            log.error("Failed to read Confluence page {}: {}", pageId, e.getMessage());
            throw new RuntimeException("Confluence API Error: " + e.getMessage(), e);
        }
    }
}
