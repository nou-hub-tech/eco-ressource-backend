package com.marketplace.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/ai")
public class AIProxyController {

    @Value("${groq.api.key:}")
    private String groqApiKey;

    // HuggingFace key — already confirmed working for broken product detection
    @Value("${huggingface.api.key:${HUGGINGFACEHUB_API_TOKEN:}}")
    private String hfApiKey;

    private static final String GROQ_URL         = "https://api.groq.com/openai/v1/chat/completions";
    private static final String GROQ_VISION_MODEL = "meta-llama/llama-4-scout-17b-16e-instruct";
    private static final String GROQ_TEXT_MODEL   = "llama-3.1-8b-instant";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public AIProxyController(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ── TEXT CHAT ────────────────────────────────────────────────────────────
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, Object> body) {
        try {
            String inputs = body.getOrDefault("inputs", "").toString();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + groqApiKey);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", GROQ_TEXT_MODEL);
            requestBody.put("messages", Arrays.asList(
                    Map.of("role", "user", "content", inputs)
            ));
            requestBody.put("max_tokens", 500);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_URL, request, Map.class);

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── IMAGE RECOGNITION via Groq Vision (used by admin broken-product flow) ─
    @PostMapping("/analyze-image")
    public ResponseEntity<Map<String, Object>> analyzeImage(@RequestParam("image") MultipartFile image) {
        try {
            String base64Image = Base64.getEncoder().encodeToString(image.getBytes());
            String dataUrl = "data:" + image.getContentType() + ";base64," + base64Image;

            boolean useGroq = groqApiKey != null && !groqApiKey.isBlank() && !groqApiKey.startsWith("${");
            if (!useGroq) {
                return ResponseEntity.status(503).body(Map.of(
                        "error", "No Groq API key configured. Set groq.api.key in application.properties."
                ));
            }

            Map<String, Object> textPart = new HashMap<>();
            textPart.put("type", "text");
            textPart.put("text",
                    "Look at this image carefully. Identify the specific product shown. " +
                            "Return ONLY a short product description (2-5 words), for example: " +
                            "'ceramic coffee mug', 'plastic water bottle', 'aluminium sheet', 'iPhone smartphone', 'cardboard box'. " +
                            "Do not add any explanation or punctuation — just the product name.");

            Map<String, Object> imageUrlObj = new HashMap<>();
            imageUrlObj.put("url", dataUrl);
            imageUrlObj.put("detail", "low");

            Map<String, Object> imagePart = new HashMap<>();
            imagePart.put("type", "image_url");
            imagePart.put("image_url", imageUrlObj);

            Map<String, Object> message = new HashMap<>();
            message.put("role", "user");
            message.put("content", Arrays.asList(textPart, imagePart));

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", GROQ_VISION_MODEL);
            requestBody.put("messages", Arrays.asList(message));
            requestBody.put("max_tokens", 60);
            requestBody.put("temperature", 0.1);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + groqApiKey);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
            System.out.println("[Vision] Calling Groq with model " + GROQ_VISION_MODEL);

            ResponseEntity<Map> response = restTemplate.postForEntity(GROQ_URL, request, Map.class);
            Map<String, Object> responseBody = response.getBody();
            System.out.println("[Vision] Status: " + response.getStatusCode());

            return ResponseEntity.ok(responseBody != null ? responseBody : Map.of("error", "Empty response"));

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            System.err.println("[Vision] HTTP error: " + e.getStatusCode() + " — " + e.getResponseBodyAsString());
            return ResponseEntity.status(e.getStatusCode()).body(Map.of(
                    "error", "Vision API error: " + e.getStatusCode(),
                    "detail", e.getResponseBodyAsString()
            ));
        } catch (Exception e) {
            System.err.println("[Vision] Exception: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── IMAGE CLASSIFICATION via HuggingFace (enterprise chatbot) ────────────
    // Uses the confirmed-working HF key (same one used for broken product detection)
    @PostMapping("/classify-image")
    public ResponseEntity<?> classifyImage(@RequestParam("image") MultipartFile image) {
        try {
            String hfUrl = "https://router.huggingface.co/hf-inference/models/google/vit-base-patch16-224";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + hfApiKey);   // HF key — NOT Groq key
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<byte[]> request = new HttpEntity<>(image.getBytes(), headers);
            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    hfUrl, HttpMethod.POST, request, String.class
            );

            String body = rawResponse.getBody();
            if (body == null || body.contains("estimated_time")) {
                return ResponseEntity.status(503).body(Map.of("error", "Model warming up", "estimated_time", true));
            }

            List<Map<String, Object>> predictions = objectMapper.readValue(
                    body, new com.fasterxml.jackson.core.type.TypeReference<>() {}
            );

            String topLabel = (String) predictions.get(0).get("label");
            double topScore = ((Number) predictions.get(0).get("score")).doubleValue();

            List<String> topLabels = predictions.stream()
                    .limit(3)
                    .map(p -> (String) p.get("label"))
                    .collect(Collectors.toList());

            System.out.println("[Classify] Top label: " + topLabel + " (" + String.format("%.1f", topScore * 100) + "%)");

            return ResponseEntity.ok(Map.of(
                    "detectedLabel", topLabel,
                    "confidence",    topScore,
                    "topLabels",     topLabels,
                    "allPredictions", predictions
            ));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── DEBUG ─────────────────────────────────────────────────────────────────
    @GetMapping("/debug")
    public ResponseEntity<Map<String, Object>> debug() {
        Map<String, Object> debug = new HashMap<>();
        debug.put("groq_api_key_status", groqApiKey == null ? "NULL" :
                (groqApiKey.isBlank() ? "EMPTY" : "SET (length: " + groqApiKey.length() + ")"));
        debug.put("hf_api_key_status", hfApiKey == null ? "NULL" :
                (hfApiKey.isBlank() ? "EMPTY" : "SET (length: " + hfApiKey.length() + ")"));
        debug.put("groq_vision_model", GROQ_VISION_MODEL);
        Map<String, String> env = System.getenv();
        debug.put("env_vars_starting_with_G",
                env.keySet().stream().filter(k -> k.startsWith("G")).collect(Collectors.toList()));
        return ResponseEntity.ok(debug);
    }
}