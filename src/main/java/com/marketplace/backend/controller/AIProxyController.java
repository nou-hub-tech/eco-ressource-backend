package com.marketplace.backend.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai")
@CrossOrigin(origins = "http://localhost:4200")
public class AIProxyController {

    // Read from environment variable (no hardcoded value)
    @Value("${GROQ_API_KEY:}")
    private String groqApiKey;

    private static final String GROQ_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final RestTemplate restTemplate;

    public AIProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        try {
            String inputs = body.getOrDefault("inputs", "").toString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + groqApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "llama-3.1-8b-instant");
            requestBody.put("messages", Arrays.asList(
                    Map.of("role", "user", "content", inputs)
            ));

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_URL, request, Map.class);

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }
    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debug() {
        Map<String, Object> debug = new HashMap<>();

        // Check @Value injection
        debug.put("value_annotation_key", groqApiKey == null ? "NULL" :
                (groqApiKey.isEmpty() ? "EMPTY" : "SET (length: " + groqApiKey.length() + ")"));

        // Check System.getenv
        String envKey = System.getenv("GROQ_API_KEY");
        debug.put("system_env_key", envKey == null ? "NULL" :
                (envKey.isEmpty() ? "EMPTY" : "SET (length: " + envKey.length() + ")"));

        // List all environment variables (be careful - remove in production)
        Map<String, String> env = System.getenv();
        debug.put("all_env_vars_starting_with_G",
                env.keySet().stream()
                        .filter(key -> key.startsWith("G"))
                        .collect(Collectors.toList()));

        // Check system properties
        debug.put("system_property_key", System.getProperty("GROQ_API_KEY"));

        return ResponseEntity.ok(debug);
    }




    
}