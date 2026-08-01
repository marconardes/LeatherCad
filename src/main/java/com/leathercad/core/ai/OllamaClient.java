package com.leathercad.core.ai;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class OllamaClient {

    private final HttpClient client;

    public OllamaClient() {
        this.client = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(2))
            .build();
    }

    public boolean isOllamaAvailable(String baseUrl) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/tags"))
                .timeout(Duration.ofSeconds(2))
                .GET()
                .build();

            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            return response.statusCode() == 200;
        } catch (Exception e) {
            return false;
        }
    }

    public String generateCompletion(String baseUrl, String modelName, String systemPrompt, String userPrompt) throws Exception {
        String jsonPayload = String.format(
            "{\"model\":\"%s\",\"system\":\"%s\",\"prompt\":\"%s\",\"stream\":false}",
            escapeJson(modelName),
            escapeJson(systemPrompt),
            escapeJson(userPrompt)
        );

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(baseUrl + "/api/generate"))
            .timeout(Duration.ofSeconds(30))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
            .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 200) {
            String body = response.body();
            int responseIdx = body.indexOf("\"response\":\"");
            if (responseIdx != -1) {
                int start = responseIdx + 12;
                int end = body.indexOf("\",\"done\":", start);
                if (end != -1) {
                    return body.substring(start, end).replace("\\n", "\n").replace("\\\"", "\"");
                }
            }
            return body;
        } else {
            throw new RuntimeException("Ollama HTTP Error: " + response.statusCode());
        }
    }

    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", " ")
            .replace("\r", " ");
    }
}
