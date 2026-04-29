package com.marketplace.backend.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import com.marketplace.backend.dto.AIDescriptionRequest;

import java.util.*;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    @Value("${groq.api.key}")
    private String groqApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/generate-description")
    public ResponseEntity<Map<String, String>> generateDescription(
            @RequestBody AIDescriptionRequest request) {

        String prompt = buildPrompt(request);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(groqApiKey);

        Map<String, Object> body = new HashMap<>();
        body.put("model", "llama-3.3-70b-versatile");
        body.put("max_tokens", 500);
        body.put("temperature", 0.7);
        body.put("messages", List.of(
            Map.of("role", "system", "content", "You are a professional event copywriter."),
            Map.of("role", "user", "content", prompt)
        ));

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<Map> groqResponse = restTemplate.postForEntity(
                "https://api.groq.com/openai/v1/chat/completions",
                entity,
                Map.class
            );

            List<Map<String, Object>> choices =
                (List<Map<String, Object>>) groqResponse.getBody().get("choices");
            Map<String, Object> message =
                (Map<String, Object>) choices.get(0).get("message");
            String generated = (String) message.get("content");

            return ResponseEntity.ok(Map.of("description", generated.trim()));

       } catch (Exception e) {
    // This will print the real cause in the Spring Boot console
    System.err.println("=== AiController ERROR: " + e.getClass().getName() + ": " + e.getMessage());
    e.printStackTrace();
    return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
        .body(Map.of("error", "Failed to contact AI service: " + e.getMessage()));
}
    }

    private String buildPrompt(AIDescriptionRequest r) {
        String draft = (r.getCurrentDescription() != null && !r.getCurrentDescription().isBlank())
            ? "- Current draft: \"" + r.getCurrentDescription() + "\""
            : "- No draft provided, create from scratch based on other context.";

        return """
            You are an event description writer for a B2B circular economy marketplace called EcoRessource.
            Write a professional, engaging event description.

            Context:
            - Event title: "%s"
            - Event type: "%s"
            - Location: "%s"
            - Date: "%s"
            %s

            Requirements:
            - Write 2-3 paragraphs
            - Professional tone suitable for B2B audience
            - Mention sustainability and circular economy themes where appropriate
            - Include a call to action
            - Do NOT use markdown formatting
            - Return ONLY the description text, no titles or headers
            """.formatted(r.getTitle(), r.getTypeLabel(), r.getLocation(), r.getEventDate(), draft);
    }
}