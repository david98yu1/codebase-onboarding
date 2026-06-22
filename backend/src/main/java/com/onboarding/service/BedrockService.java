package com.onboarding.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

@Service
public class BedrockService {

    private static final String API_URL = "https://4dm65e698a.execute-api.us-west-2.amazonaws.com/prod/invoke";
    private static final String MODEL    = "claude-sonnet-4.5";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${classroom.apiKey}")
    private String apiKey;

    public String invoke(String prompt) {
        try {
            Map<String, Object> body = Map.of(
                "model",     MODEL,
                "input",     prompt,
                "maxTokens", 2048
            );

            String bodyJson = objectMapper.writeValueAsString(body);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .header("x-api-key", apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(bodyJson))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new RuntimeException("API returned status " + response.statusCode() + ": " + response.body());
            }

            // Response shape: { "output": "..." }
            Map<?, ?> parsed = objectMapper.readValue(response.body(), Map.class);
            return (String) parsed.get("output");

        } catch (Exception e) {
            throw new RuntimeException("API invocation failed: " + e.getMessage(), e);
        }
    }
}
