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

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/broken-product")
public class BrokenProductController {

    @Value("${huggingface.api.key}")
    private String hfApiKey;

    private final IProductRepository productRepository;
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Define priority order for products (higher number = higher priority)
    private static final Map<String, Integer> PRODUCT_PRIORITY = new HashMap<>();

    static {
        // Products that should be preferred when scores are equal
        PRODUCT_PRIORITY.put("iphone", 100);
        PRODUCT_PRIORITY.put("mug", 90);
        PRODUCT_PRIORITY.put("notebook", 50);
        PRODUCT_PRIORITY.put("laptop", 50);
        PRODUCT_PRIORITY.put("bottle", 80);
        // Default priority for other products is 0
    }

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

            if (body == null || body.contains("estimated_time")) {
                return ResponseEntity.status(503).body(
                        Map.of("error", "Model is warming up. Please wait 20 seconds and try again.")
                );
            }

            List<Map<String, Object>> predictions = objectMapper.readValue(
                    body, new TypeReference<List<Map<String, Object>>>() {}
            );

            if (predictions == null || predictions.isEmpty()) {
                return ResponseEntity.status(500).body(Map.of("error", "No predictions returned."));
            }

            String topLabel = (String) predictions.get(0).get("label");
            double topScore = ((Number) predictions.get(0).get("score")).doubleValue();

            List<Product> allProducts = productRepository.findAll();

            // Priority mapping for better matching
            Map<String, List<String>> PRIORITY_MATCHES = new HashMap<>();
            PRIORITY_MATCHES.put("iphone", Arrays.asList("ipod", "smartphone", "phone", "mobile phone", "cell phone"));
            PRIORITY_MATCHES.put("ipod", Arrays.asList("iphone", "music player", "mp3 player"));
            PRIORITY_MATCHES.put("mug", Arrays.asList("cup", "coffee mug", "tea cup", "measuring cup"));
            PRIORITY_MATCHES.put("notebook", Arrays.asList("laptop", "computer"));
            PRIORITY_MATCHES.put("laptop", Arrays.asList("notebook", "computer"));

            // Calculate match score for EACH product against ALL predictions
            List<ProductMatch> productMatches = new ArrayList<>();

            for (Product product : allProducts) {
                String productName = product.getName().toLowerCase().trim();
                String productCategory = product.getCategory() != null ? product.getCategory().toLowerCase().trim() : "";

                double bestScoreForThisProduct = 0;
                String bestMatchingLabel = null;
                double bestAIConfidence = 0;
                String matchType = "";

                // Check this product against every AI prediction
                for (Map<String, Object> prediction : predictions) {
                    String label = (String) prediction.get("label");
                    double aiConfidence = ((Number) prediction.get("score")).doubleValue();
                    String labelLower = label.toLowerCase().trim();

                    MatchQuality matchQuality = calculateMatchQuality(productName, productCategory, labelLower, PRIORITY_MATCHES);

                    if (matchQuality.getScore() > 0) {
                        double finalScore = matchQuality.getScore();

                        // For high-quality semantic matches, give it high score regardless of AI confidence
                        if (matchQuality.getMatchType().equals("PRIORITY") || matchQuality.getMatchType().equals("SEMANTIC")) {
                            finalScore = finalScore * 100;
                        } else {
                            finalScore = finalScore * aiConfidence * 100;
                        }

                        if (finalScore > bestScoreForThisProduct) {
                            bestScoreForThisProduct = finalScore;
                            bestMatchingLabel = label;
                            bestAIConfidence = aiConfidence;
                            matchType = matchQuality.getMatchType();
                        }
                    }
                }

                if (bestScoreForThisProduct > 0) {
                    // Get product priority (default 0)
                    int priority = PRODUCT_PRIORITY.getOrDefault(productName, 0);
                    productMatches.add(new ProductMatch(product, bestScoreForThisProduct, bestMatchingLabel, bestAIConfidence, matchType, priority));
                }
            }

            // Sort by score (descending), then by priority (descending), then by AI confidence (descending)
            productMatches.sort((a, b) -> {
                // First compare by score
                if (a.getScore() != b.getScore()) {
                    return Double.compare(b.getScore(), a.getScore());
                }
                // If scores are equal, compare by priority (higher priority wins)
                if (a.getPriority() != b.getPriority()) {
                    return Integer.compare(b.getPriority(), a.getPriority());
                }
                // If priorities are equal, compare by AI confidence
                return Double.compare(b.getAiConfidence(), a.getAiConfidence());
            });

            // Get the best match
            ProductMatch bestMatch = productMatches.isEmpty() ? null : productMatches.get(0);

            // Prepare result
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("detectedLabel", topLabel);
            result.put("detectedConfidence", String.format("%.1f%%", topScore * 100));
            result.put("allPredictions", predictions);

            if (bestMatch != null && bestMatch.getScore() > 0) {
                result.put("matchedProduct", bestMatch.getProduct());
                result.put("matchedFromLabel", bestMatch.getMatchedLabel());
                result.put("matchedConfidence", String.format("%.1f%%", bestMatch.getAiConfidence() * 100));
                result.put("matchScore", String.format("%.2f", bestMatch.getScore()));
                result.put("matchType", bestMatch.getMatchType());
                result.put("matchFound", true);

                // Add explanation
                result.put("matchExplanation", getMatchExplanation(
                        bestMatch.getProduct().getName(),
                        bestMatch.getMatchedLabel(),
                        bestMatch.getMatchType()
                ));
            } else {
                result.put("matchFound", false);
                result.put("message", "No product found in database matching this image.");
            }

            // Debug info - show all matches sorted
            List<Map<String, Object>> debugInfo = new ArrayList<>();
            for (ProductMatch pm : productMatches) {
                Map<String, Object> debug = new HashMap<>();
                debug.put("productName", pm.getProduct().getName());
                debug.put("bestMatchLabel", pm.getMatchedLabel());
                debug.put("score", pm.getScore());
                debug.put("matchType", pm.getMatchType());
                debug.put("priority", pm.getPriority());
                debug.put("aiConfidence", String.format("%.1f%%", pm.getAiConfidence() * 100));
                debugInfo.add(debug);
            }
            result.put("debugInfo", debugInfo);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    private MatchQuality calculateMatchQuality(String productName, String productCategory, String predictedLabel, Map<String, List<String>> priorityMatches) {
        double score = 0;
        String matchType = "NONE";

        // 1. PRIORITY MATCH - Check if this product should be prioritized for this prediction
        for (Map.Entry<String, List<String>> entry : priorityMatches.entrySet()) {
            String product = entry.getKey();
            List<String> matches = entry.getValue();

            if (productName.equals(product)) {
                for (String match : matches) {
                    if (predictedLabel.contains(match) || match.contains(predictedLabel)) {
                        score = 1.0;
                        matchType = "PRIORITY";
                        return new MatchQuality(score, matchType);
                    }
                }
            }
        }

        // 2. EXACT MATCH
        if (productName.equals(predictedLabel)) {
            score = 1.0;
            matchType = "EXACT";
        }
        // 3. CONTAINS MATCH
        else if (productName.contains(predictedLabel) || predictedLabel.contains(productName)) {
            score = 0.9;
            matchType = "CONTAINS";
        }
        // 4. CATEGORY MATCH
        else if (!productCategory.isEmpty() && (productCategory.contains(predictedLabel) || predictedLabel.contains(productCategory))) {
            score = 0.8;
            matchType = "CATEGORY";
        }
        // 5. SEMANTIC MATCH - iPhone/iPod specific
        else if (productName.equals("iphone") && (predictedLabel.equals("ipod") || predictedLabel.contains("pod"))) {
            score = 0.95;
            matchType = "SEMANTIC";
        }
        else if (productName.equals("ipod") && (predictedLabel.equals("iphone") || predictedLabel.contains("phone"))) {
            score = 0.95;
            matchType = "SEMANTIC";
        }
        // 6. Mug/Cup semantic match
        else if (productName.equals("mug") && (predictedLabel.contains("cup") || predictedLabel.equals("coffee mug") || predictedLabel.equals("measuring cup"))) {
            score = 0.95;
            matchType = "SEMANTIC";
        }
        // 7. Electronic device similarity
        else if (isElectronicDevice(productName) && isElectronicDevice(predictedLabel)) {
            score = 0.85;
            matchType = "SEMANTIC";
        }
        // 8. Partial word match
        else if (hasCommonSubstring(productName, predictedLabel, 3)) {
            score = 0.5;
            matchType = "PARTIAL";
        }

        return new MatchQuality(score, matchType);
    }

    private boolean isElectronicDevice(String word) {
        String[] devices = {"iphone", "ipod", "ipad", "phone", "smartphone", "android", "samsung", "pixel", "laptop", "notebook", "computer"};
        for (String device : devices) {
            if (word.contains(device)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasCommonSubstring(String str1, String str2, int minLength) {
        str1 = str1.toLowerCase();
        str2 = str2.toLowerCase();

        for (int i = 0; i <= str1.length() - minLength; i++) {
            String substring = str1.substring(i, i + minLength);
            if (str2.contains(substring)) {
                return true;
            }
        }
        return false;
    }

    private String getMatchExplanation(String productName, String matchedLabel, String matchType) {
        if (matchType.equals("PRIORITY") || matchType.equals("SEMANTIC")) {
            if (productName.equalsIgnoreCase("iphone") && matchedLabel.equalsIgnoreCase("ipod")) {
                return "✅ The AI detected an 'iPod'. Since you have an 'iPhone' in your database (higher priority than notebook), we matched with iPhone!";
            }
            if (productName.equalsIgnoreCase("mug") && (matchedLabel.contains("cup") || matchedLabel.equals("coffee mug"))) {
                return "✅ The AI detected a '" + matchedLabel + "', which is essentially the same as a mug.";
            }
            return "✅ Smart match: '" + matchedLabel + "' is semantically similar to '" + productName + "'.";
        }
        return "Matched based on text similarity between '" + matchedLabel + "' and '" + productName + "'.";
    }

    // Helper class for product match results
    class ProductMatch {
        private Product product;
        private double score;
        private String matchedLabel;
        private double aiConfidence;
        private String matchType;
        private int priority;

        public ProductMatch(Product product, double score, String matchedLabel, double aiConfidence, String matchType, int priority) {
            this.product = product;
            this.score = score;
            this.matchedLabel = matchedLabel;
            this.aiConfidence = aiConfidence;
            this.matchType = matchType;
            this.priority = priority;
        }

        public Product getProduct() { return product; }
        public double getScore() { return score; }
        public String getMatchedLabel() { return matchedLabel; }
        public double getAiConfidence() { return aiConfidence; }
        public String getMatchType() { return matchType; }
        public int getPriority() { return priority; }
    }

    class MatchQuality {
        private double score;
        private String matchType;

        public MatchQuality(double score, String matchType) {
            this.score = score;
            this.matchType = matchType;
        }

        public double getScore() { return score; }
        public String getMatchType() { return matchType; }
    }
}