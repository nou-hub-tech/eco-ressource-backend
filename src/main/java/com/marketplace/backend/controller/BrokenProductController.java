package com.marketplace.backend.controller;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.marketplace.backend.entity.Product;
import com.marketplace.backend.repository.IProductRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/broken-product")
@CrossOrigin(origins = "http://localhost:4200")
public class BrokenProductController {

    @Value("${huggingface.api.key}")
    private String hfApiKey;

    private final IProductRepository productRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BrokenProductController(IProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @PostMapping("/detect")
    public ResponseEntity<?> detect(@RequestParam("image") MultipartFile image) {
        try {
            String hfUrl = "https://router.huggingface.co/hf-inference/models/google/vit-base-patch16-224";
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + hfApiKey);
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setAccept(List.of(MediaType.APPLICATION_JSON));

            HttpEntity<byte[]> request = new HttpEntity<>(image.getBytes(), headers);

            ResponseEntity<String> rawResponse = restTemplate.exchange(
                    hfUrl, HttpMethod.POST, request, String.class
            );

            String body = rawResponse.getBody();
            System.out.println("==== HF RAW RESPONSE ====");
            System.out.println(body);
            System.out.println("=========================");

            // Check if model is still loading
            if (body == null || body.contains("estimated_time")) {
                return ResponseEntity.status(503).body(
                        Map.of("error", "Model is warming up. Please wait 20 seconds and try again.")
                );
            }

            // Parse predictions
            List<Map<String, Object>> predictions = objectMapper.readValue(
                    body, new TypeReference<List<Map<String, Object>>>() {}
            );

            if (predictions == null || predictions.isEmpty()) {
                return ResponseEntity.status(500).body(Map.of("error", "No predictions returned."));
            }

            String topLabel = (String) predictions.get(0).get("label");
            double topScore = ((Number) predictions.get(0).get("score")).doubleValue();

            // Match against DB
            List<Product> allProducts = productRepository.findAll();
            Product matched = allProducts.stream()
                    .filter(p ->
                            p.getName().toLowerCase().contains(topLabel.toLowerCase()) ||
                                    topLabel.toLowerCase().contains(p.getName().toLowerCase()) ||
                                    p.getCategory().toLowerCase().contains(topLabel.toLowerCase())
                    )
                    .findFirst()
                    .orElse(null);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("detectedLabel", topLabel);
            result.put("confidence", String.format("%.1f%%", topScore * 100));
            result.put("allPredictions", predictions);
            result.put("matchedProduct", matched);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}