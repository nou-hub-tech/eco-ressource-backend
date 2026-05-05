package com.marketplace.backend.controller;

import com.marketplace.backend.dto.ReclamationResponseDTO;
import com.marketplace.backend.entity.*;
import com.marketplace.backend.repository.*;
import com.marketplace.backend.service.FileStorageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/enterprise/reclamations")
public class DefectAnalysisController {

    @Value("${huggingface.api.key:}")
    private String hfApiKey;

    @Value("${groq.api.key:}")
    private String groqApiKey;

    private final UserRepository userRepository;
    private final EnterpriseRepository enterpriseRepository;
    private final ReclamationRepository reclamationRepository;
    private final IProductRepository productRepository;
    private final IStockItemRepository stockItemRepository;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DefectAnalysisController(UserRepository userRepository,
                                    EnterpriseRepository enterpriseRepository,
                                    ReclamationRepository reclamationRepository,
                                    IProductRepository productRepository,
                                    IStockItemRepository stockItemRepository,
                                    FileStorageService fileStorageService) {
        this.userRepository = userRepository;
        this.enterpriseRepository = enterpriseRepository;
        this.reclamationRepository = reclamationRepository;
        this.productRepository = productRepository;
        this.stockItemRepository = stockItemRepository;
        this.fileStorageService = fileStorageService;
    }

    // ── Helper ──────────────────────────────────────────────────────────────
    private Enterprise getEnterprise(Authentication auth) {
        String email = auth.getName();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "User not found"));
        return enterpriseRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Not linked to an enterprise"));
    }

    private String buildImageUrl(String raw) {
        if (raw == null) return null;
        if (raw.startsWith("http")) return raw;
        return "http://localhost:9090/files/" + raw;
    }

    // ── CREATE ───────────────────────────────────────────────────────────────
    @PostMapping("/analyze-and-create")
    public ResponseEntity<?> analyzeAndCreateReclamation(
            @RequestParam(value = "stockItemId",    required = false)           Long stockItemId,
            @RequestParam(value = "productId",      required = false)           Long productId,
            @RequestParam("description")                                          String description,
            @RequestParam(value = "damagedQuantity", defaultValue = "1")         Integer damagedQuantity,
            @RequestParam(value = "image",          required = false)            MultipartFile image,
            Authentication auth) {

        try {
            Enterprise enterprise = getEnterprise(auth);
            Product   product   = null;
            StockItem stockItem = null;

            if (stockItemId != null) {
                stockItem = stockItemRepository.findById(stockItemId).orElse(null);
                if (stockItem != null) product = stockItem.getProduct();
            } else if (productId != null) {
                product = productRepository.findById(productId).orElse(null);
            }

            if (product == null)
                return ResponseEntity.badRequest().body(Map.of("error", "Product not found."));

            // Determine target enterprise (stock owner)
            Enterprise targetEnterprise = null;
            if (stockItem != null && stockItem.getEnterprise() != null)
                targetEnterprise = stockItem.getEnterprise();
            else if (product.getEnterprise() != null)
                targetEnterprise = product.getEnterprise();

            // Save image
            String imageUrl = null;
            if (image != null && !image.isEmpty()) {
                try { imageUrl = fileStorageService.storeFile(image); }
                catch (Exception e) { System.err.println("Image save failed: " + e.getMessage()); }
            }

            // AI analysis
            String combined    = description.toLowerCase();
            String defectType  = classifyDefect(combined, product.getName(), product.getCategory(), product.getMaterialType());
            String severity    = getSeverity(defectType);
            String recs        = getRecommendations(defectType, product.getName());
            double confidence  = 0.70;
            String aiAnalysis  = buildFullAnalysis(defectType, severity, product.getName(), description, stockItem);

            // Enrich with Groq if available
            boolean groqEnabled = groqApiKey != null && !groqApiKey.isBlank() && !groqApiKey.startsWith("${");
            if (groqEnabled) {
                try {
                    String groqResult = callGroq(product.getName(), product.getCategory(),
                            product.getMaterialType(), description, defectType, stockItem);
                    if (groqResult != null && !groqResult.isBlank()) {
                        aiAnalysis = groqResult;
                        confidence = 0.88;
                    }
                } catch (Exception ge) {
                    System.err.println("[AI] Groq failed: " + ge.getMessage());
                }
            }

            // Save reclamation (truncate aiAnalysis as safety net — column is TEXT but just in case)
            String safeAiAnalysis = aiAnalysis != null && aiAnalysis.length() > 10000
                    ? aiAnalysis.substring(0, 10000)
                    : aiAnalysis;

            Reclamation reclamation = Reclamation.builder()
                    .enterprise(enterprise)
                    .targetEnterprise(targetEnterprise)
                    .stockItem(stockItem)
                    .product(product)
                    .description(description)
                    .defectType(defectType)
                    .status("PENDING")
                    .imageUrl(imageUrl)
                    .damagedQuantity(damagedQuantity)
                    .resolutionNotes(safeAiAnalysis)
                    .build();

            reclamation = reclamationRepository.save(reclamation);

            ReclamationResponseDTO response = ReclamationResponseDTO.builder()
                    .id(reclamation.getId())
                    .productName(product.getName())
                    .description(description)
                    .defectType(defectType)
                    .status("PENDING")
                    .createdAt(reclamation.getCreatedAt())
                    .aiAnalysis(aiAnalysis)
                    .confidence(confidence)
                    .recommendations(recs)
                    .imageUrl(buildImageUrl(imageUrl))
                    .build();

            return ResponseEntity.ok(Map.of("success", true, "reclamation", response));

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GROQ CALL ────────────────────────────────────────────────────────────
    private String callGroq(String productName, String category, String material,
                            String description, String defectType, StockItem stockItem) throws Exception {

        String prompt = "You are a quality control expert for recycled materials and industrial products. "
                + "A defect claim was filed. Product details:\n"
                + "- Name: '" + productName + "'\n"
                + "- Category: '" + category + "'\n"
                + "- Material: '" + material + "'\n"
                + (stockItem != null ? "- Stock Location: '" + (stockItem.getLocation() != null ? stockItem.getLocation() : "N/A") + "'\n" : "")
                + (stockItem != null ? "- Condition at time of stock entry: '" + (stockItem.getCondition() != null ? stockItem.getCondition() : "N/A") + "'\n" : "")
                + (stockItem != null ? "- Quantity in stock: " + stockItem.getQuantity() + " " + (stockItem.getUnit() != null ? stockItem.getUnit() : "") + "\n" : "")
                + (stockItem != null ? "- Unit price: " + stockItem.getUnitPrice() + " TND\n" : "")
                + "User description of the defect: '" + description + "'\n"
                + "Detected defect type: '" + defectType + "'\n\n"
                + "In 3-4 sentences, confirm the defect classification, assess severity based on the product's "
                + "material and condition, estimate the financial impact, and give a concrete recommendation. "
                + "Be direct and professional.";

        // Build JSON manually to avoid escaping issues
        String safePrompt = prompt.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n");
        String body = "{\"model\":\"llama-3.1-8b-instant\","
                + "\"messages\":[{\"role\":\"user\",\"content\":\"" + safePrompt + "\"}],"
                + "\"max_tokens\":200}";

        java.net.URL url = new java.net.URL("https://api.groq.com/openai/v1/chat/completions");
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + groqApiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(15000);

        try (java.io.OutputStream os = conn.getOutputStream()) {
            os.write(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }

        int status = conn.getResponseCode();
        java.io.InputStream is = (status < 400) ? conn.getInputStream() : conn.getErrorStream();
        String responseBody = new String(is.readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
        System.out.println("[AI] Groq status: " + status);

        if (status == 200) {
            // Parse {"choices":[{"message":{"content":"TEXT"}}]}
            List<Map<String, Object>> choices = null;
            try {
                Map<String, Object> json = objectMapper.readValue(responseBody, new TypeReference<>() {});
                choices = (List<Map<String, Object>>) json.get("choices");
            } catch (Exception ignored) {}

            if (choices != null && !choices.isEmpty()) {
                Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
                if (message != null && message.get("content") != null)
                    return message.get("content").toString().trim();
            }
        }
        System.err.println("[AI] Groq response: " + responseBody.substring(0, Math.min(200, responseBody.length())));
        return null;
    }

    // ── GET SENT ─────────────────────────────────────────────────────────────
    @GetMapping("/my")
    public ResponseEntity<?> getMyReclamations(Authentication auth) {
        try {
            Enterprise enterprise = getEnterprise(auth);
            List<Reclamation> list = reclamationRepository.findByEnterpriseIdOrderByCreatedAtDesc(enterprise.getId());
            List<Map<String, Object>> response = new ArrayList<>();
            for (Reclamation r : list) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id",              r.getId());
                item.put("productName",     r.getProduct() != null ? r.getProduct().getName() : "Unknown");
                item.put("description",     r.getDescription());
                item.put("defectType",      r.getDefectType());
                item.put("status",          r.getStatus());
                item.put("createdAt",       r.getCreatedAt());
                item.put("imageUrl",        buildImageUrl(r.getImageUrl()));
                item.put("aiAnalysis",      r.getResolutionNotes());
                item.put("damagedQuantity", r.getDamagedQuantity());
                item.put("damagedUnit",     r.getDamagedUnit());
                // Product category & material for context
                if (r.getProduct() != null) {
                    item.put("productCategory",   r.getProduct().getCategory());
                    item.put("productMaterial",   r.getProduct().getMaterialType());
                }
                // Stock details if available
                if (r.getStockItem() != null) {
                    item.put("stockLocation",  r.getStockItem().getLocation());
                    item.put("stockCondition", r.getStockItem().getCondition());
                    item.put("stockQty",       r.getStockItem().getQuantity());
                    item.put("stockUnit",      r.getStockItem().getUnit());
                    item.put("unitPrice",      r.getStockItem().getUnitPrice());
                }
                response.add(item);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET RECEIVED ──────────────────────────────────────────────────────────
    @GetMapping("/received")
    public ResponseEntity<?> getReceivedReclamations(Authentication auth) {
        try {
            Enterprise enterprise = getEnterprise(auth);
            List<Reclamation> list = reclamationRepository.findReceivedByEnterpriseId(enterprise.getId());
            List<Map<String, Object>> response = new ArrayList<>();
            for (Reclamation r : list) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id",              r.getId());
                item.put("productName",     r.getProduct() != null ? r.getProduct().getName() : "Unknown");
                item.put("description",     r.getDescription());
                item.put("defectType",      r.getDefectType());
                item.put("status",          r.getStatus());
                item.put("createdAt",       r.getCreatedAt());
                item.put("imageUrl",        buildImageUrl(r.getImageUrl()));
                item.put("damagedQuantity", r.getDamagedQuantity());
                item.put("stockItemId",     r.getStockItem() != null ? r.getStockItem().getIdStock()  : null);
                item.put("stockQty",        r.getStockItem() != null ? r.getStockItem().getQuantity() : null);
                item.put("claimantName",    r.getEnterprise() != null ? r.getEnterprise().getCompanyName() : "Unknown");
                response.add(item);
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── TREAT ─────────────────────────────────────────────────────────────────
    @PutMapping("/{id}/treat")
    public ResponseEntity<?> treatReclamation(@PathVariable Long id, Authentication auth) {
        try {
            Enterprise enterprise = getEnterprise(auth);
            Reclamation reclamation = reclamationRepository.findById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Reclamation not found"));

            if (reclamation.getTargetEnterprise() == null ||
                    !reclamation.getTargetEnterprise().getId().equals(enterprise.getId()))
                return ResponseEntity.status(403).body(Map.of("error", "You are not the owner of this stock"));

            if ("TREATED".equals(reclamation.getStatus()))
                return ResponseEntity.badRequest().body(Map.of("error", "Already treated"));

            int newQty = 0;
            StockItem stockItem = reclamation.getStockItem();
            if (stockItem != null) {
                int decrease = reclamation.getDamagedQuantity() != null ? reclamation.getDamagedQuantity() : 1;
                newQty = Math.max(0, stockItem.getQuantity() - decrease);
                stockItem.setQuantity(newQty);
                stockItemRepository.save(stockItem);
            }

            reclamation.setStatus("TREATED");
            reclamation.setResolvedAt(java.time.LocalDateTime.now());
            String note = "Approved by stock owner. Stock decreased by "
                    + (reclamation.getDamagedQuantity() != null ? reclamation.getDamagedQuantity() : 1)
                    + ". New quantity: " + newQty + ".";
            reclamation.setResolutionNotes(
                    (reclamation.getResolutionNotes() != null ? reclamation.getResolutionNotes() + "\n\n" : "") + note);
            reclamationRepository.save(reclamation);

            return ResponseEntity.ok(Map.of("success", true, "message", "Stock updated.", "newStockQty", newQty));

        } catch (ResponseStatusException e) { throw e; }
        catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── DEFECT CLASSIFICATION ─────────────────────────────────────────────────
    private String classifyDefect(String text, String productName, String category, String material) {
        String p = (productName != null ? productName : "").toLowerCase();
        String c = (category   != null ? category   : "").toLowerCase();
        String m = (material   != null ? material   : "").toLowerCase();

        if (containsAny(text, "crack", "cracked", "broken", "fracture", "shatter", "split", "snap")) {
            if (containsAny(p + c + m, "glass", "ceramic", "mug", "cup", "bottle")) return "Cracked / Shattered";
            if (containsAny(p + c + m, "phone", "iphone", "screen", "display", "electronic")) return "Screen Cracked / Display Damage";
            return "Cracked / Broken";
        }
        if (containsAny(text, "dent", "dented", "bent", "deformed", "crushed", "squashed", "warp")) return "Dented / Deformed";
        if (containsAny(text, "rust", "corrosion", "oxidiz")) return "Rust / Corrosion";
        if (containsAny(text, "water", "wet", "moisture", "flood", "leak")) return "Water Damage";
        if (containsAny(text, "scratch", "scratched", "scuff", "gouge")) return "Scratches / Cosmetic Damage";
        if (containsAny(text, "stain", "stained", "discolor", "fade", "yellow", "dirty")) return "Stained / Discolored";
        if (containsAny(text, "burn", "burnt", "melt", "melted", "char", "scorch", "fire", "heat")) return "Burn / Heat Damage";
        if (containsAny(text, "torn", "tear", "cut", "rip", "hole", "puncture"))
            return containsAny(p + c, "textile", "fabric", "cloth", "bag") ? "Torn / Cut Textile" : "Torn / Punctured";
        if (containsAny(text, "missing", "incomplete", "broken off", "detached", "partial")) return "Missing Component / Part";
        if (containsAny(text, "packaging", "box", "wrap", "label", "seal")) return "Packaging Damaged";
        if (containsAny(p + c, "phone", "electronic", "iphone", "laptop", "computer")) return "Electronic Malfunction / Physical Damage";
        if (containsAny(p + c + m, "ceramic", "glass", "mug", "cup", "bottle")) return "Chipped / Cracked Ceramic or Glass";
        if (containsAny(m, "plastic") && containsAny(text, "break", "crack", "snap")) return "Cracked Plastic";
        return "Physical Damage Detected";
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) if (text.contains(kw)) return true;
        return false;
    }

    private String getSeverity(String defectType) {
        switch (defectType) {
            case "Screen Cracked / Display Damage": case "Cracked / Shattered":
            case "Water Damage": case "Burn / Heat Damage": case "Rust / Corrosion": return "High";
            case "Cracked / Broken": case "Dented / Deformed":
            case "Missing Component / Part": case "Electronic Malfunction / Physical Damage": return "Medium";
            default: return "Low";
        }
    }

    private String getRecommendations(String defectType, String productName) {
        switch (defectType) {
            case "Cracked / Shattered": case "Cracked / Broken": case "Cracked Plastic":
            case "Chipped / Cracked Ceramic or Glass": return "Product is structurally compromised. Return or recycle.";
            case "Screen Cracked / Display Damage": return "Screen replacement required. Contact supplier.";
            case "Water Damage": return "Professional inspection required before any reuse.";
            case "Rust / Corrosion": return "Recommend recycling the metal components.";
            case "Burn / Heat Damage": return "Do not reuse. Recycle if possible.";
            case "Scratches / Cosmetic Damage": return "Cosmetic only — consider a discount (10-25%).";
            case "Stained / Discolored": return "Attempt cleaning first. If it persists, return to supplier.";
            case "Dented / Deformed": return "Assess function — if functional, discount; otherwise return.";
            case "Packaging Damaged": return "Repackage if contents are intact.";
            case "Missing Component / Part": return "Check if parts can be sourced. If not, return to supplier.";
            case "Torn / Cut Textile": return "Assess repair or repurpose as recycled fibre.";
            case "Electronic Malfunction / Physical Damage": return "Contact supplier for warranty claim.";
            default: return "Manual quality inspection recommended. Document and contact your supplier.";
        }
    }

    private String buildFullAnalysis(String defectType, String severity, String productName,
                                     String userDescription, StockItem stockItem) {
        StringBuilder sb = new StringBuilder();
        sb.append("AI Analysis for ").append(productName).append(":\n\n");
        sb.append("Defect identified: ").append(defectType).append(" (").append(severity).append(" severity).\n\n");
        sb.append("User reported: \"").append(userDescription).append("\".\n\n");
        if (stockItem != null) {
            sb.append("Product details at time of claim:\n");
            if (stockItem.getLocation() != null) sb.append("• Location: ").append(stockItem.getLocation()).append("\n");
            if (stockItem.getCondition() != null) sb.append("• Condition: ").append(stockItem.getCondition()).append("\n");
            sb.append("• Stock quantity: ").append(stockItem.getQuantity()).append(" ").append(stockItem.getUnit() != null ? stockItem.getUnit() : "").append("\n");
            sb.append("• Unit price: ").append(stockItem.getUnitPrice()).append(" TND\n");
            sb.append("• Total stock value: ").append(String.format("%.2f", stockItem.getQuantity() * stockItem.getUnitPrice())).append(" TND\n\n");
        }
        sb.append("Classification based on product description and defect keywords.");
        return sb.toString();
    }
}