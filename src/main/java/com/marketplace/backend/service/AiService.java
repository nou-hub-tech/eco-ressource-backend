package com.marketplace.backend.service;

import com.marketplace.backend.entity.SolidarityAssociation;
import com.marketplace.backend.repository.SolidarityAssociationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AiService {

    private final SolidarityAssociationRepository associationRepository;
    private final DiscordBotService discordBotService;

    @Value("${app.openrouter.api-key:}")
    private String apiKey;

    @Value("${app.openrouter.model:nvidia/nemotron-3-super-120b-a12b:free}")
    private String model;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Entry point for async updates.
     * Fetches the association, generates insight, and saves back to DB.
     */
    @Async
    public void updateAssociationInsightAsync(Long associationId) {
        try {
            SolidarityAssociation association = associationRepository.findById(associationId).orElse(null);
            if (association != null) {
                System.out.println("🤖 AI: Generating asynchronous insight for Association: " + association.getName());
                String insight = generateInsight(association);

                // Fetch again in case it changed during AI call to avoid overwriting recent
                // updates
                SolidarityAssociation latest = associationRepository.findById(associationId).orElse(association);
                latest.setAiInsight(insight);
                associationRepository.save(latest);

                System.out.println("✅ AI: Insight updated successfully for Association: " + latest.getName());
                log.info("AI Insight updated asynchronously for association ID: {}", associationId);

                // Notify Discord with the fresh AI insight
                discordBotService.sendNotification("🤖 **AI Insight Ready** for **" + latest.getName() + "**:\n" + insight);
            }
        } catch (Exception e) {
            log.error("Error in async AI insight update: {}", e.getMessage());
            System.err.println("AI ASYNC ERROR: " + e.getMessage());
        }
    }

    public String generateInsight(SolidarityAssociation association) {

        if (apiKey == null || apiKey.trim().isEmpty()) {
            log.warn("OpenRouter API key is missing. AI insights will be disabled.");
            return "AI Insight currently unavailable (API key missing).";
        }

        String prompt = String.format(
                "Analyze this Tunisian association and estimate how the donations (in TND) were used locally.\n\n" +
                        "Rules:\n" +
                        "- If donations are 0 or too low, output exactly: 'No significant impact yet'\n" +
                        "- Otherwise generate exactly 2 or 3 very short impact statements\n" +
                        "- Max 10 words per line\n" +
                        "- Include a number and measurable result\n" +
                        "- Must match the mission\n" +
                        "- Tunisia context only\n" +
                        "- No explanations\n" +
                        "- Bullet list only\n\n" +
                        "Name: %s\n" +
                        "Mission: %s\n" +
                        "Donations: %.2f TND\n" +
                        "Goal: %.2f TND",
                association.getName(),
                association.getMission(),
                association.getDonations(),
                association.getGoalAmount() != null ? association.getGoalAmount() : 0.0);

        System.out.println("🤖 AI Prompt: " + prompt);
        log.debug("AI Prompt for association {}: {}", association.getId(), prompt);

        try {
            String url = "https://openrouter.ai/api/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            headers.set("HTTP-Referer", "http://localhost:8080"); // Required by OpenRouter
            headers.set("X-Title", "Eco-Ressource Backend");

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("messages", List.of(
                    Map.of("role", "user", "content", prompt)));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response != null && response.containsKey("choices")) {
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
                if (!choices.isEmpty()) {
                    Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                    String content = (String) message.get("content");
                    System.out.println("🤖 AI Response: " + content);
                    log.info("AI generated insight for association {}: {}", association.getId(), content);
                    return content;
                }
            }
            log.warn("AI returned empty response for association {}", association.getId());
            return "Unable to generate insight at this time.";

        } catch (Exception e) {
            log.error("Failed to generate AI insight for association {}: {}", association.getId(), e.getMessage());
            System.err.println("AI ERROR: " + e.getMessage());
            return "Insight generation failed: " + e.getMessage();
        }
    }
}
