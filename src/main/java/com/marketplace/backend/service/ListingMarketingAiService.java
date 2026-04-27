package com.marketplace.backend.service;

import com.marketplace.backend.dto.ListingMarketingRequest;
import com.marketplace.backend.dto.ListingMarketingSuggestion;
import com.marketplace.backend.entity.enums.ListingType;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ListingMarketingAiService {

  public ListingMarketingSuggestion suggest(ListingMarketingRequest req) {
    String product = clean(firstNonBlank(req.getProductName(), "ressource"));
    String category = clean(firstNonBlank(req.getProductCategory(), "recyclage"));
    String location = clean(firstNonBlank(req.getLocation(), "Tunisie"));
    String typeLabel = typeLabel(req.getType());
    String priceLine =
        req.getPrice() == null || req.getPrice() <= 0
            ? "Prix negociable selon volume et delai."
            : "Prix indicatif: " + req.getPrice() + " TND.";

    List<String> sellingPoints = buildSellingPoints(req, product, category, location);
    List<String> tags = buildTags(req, product, category, location);
    String materialType = detectMaterial(product + " " + category + " " + req.getDescription());
    double suggestedPrice = suggestLocalPrice(req, materialType);
    String improvedTitle =
        trimTo(
            titleCase(typeLabel + " " + product + " - " + req.getQuantity() + " " + req.getUnit()
                + " a " + location),
            120);
    String improvedDescription =
        trimTo(
            "Annonce professionnelle pour " + product + " (" + category + "). "
                + "Quantite disponible/recherchee: " + req.getQuantity() + " " + req.getUnit()
                + ". Localisation: " + location + ". "
                + String.join(" ", sellingPoints) + " "
                + priceLine + " "
                + callToAction(req.getType()),
            2000);

    return ListingMarketingSuggestion.builder()
        .improvedTitle(improvedTitle)
        .improvedDescription(improvedDescription)
        .callToAction(callToAction(req.getType()))
        .tags(tags)
        .sellingPoints(sellingPoints)
        .materialType(materialType)
        .suggestedPrice(suggestedPrice)
        .priceExplanation("Estimation IA locale basee sur le type de matiere, la quantite et le contexte de l'annonce.")
        .qualityScore(score(req))
        .build();
  }

  private List<String> buildSellingPoints(
      ListingMarketingRequest req, String product, String category, String location) {
    List<String> points = new ArrayList<>();
    points.add("Produit " + product + " trie et pret pour un usage B2B.");
    points.add("Categorie " + category + " avec informations claires sur volume et unite.");
    points.add("Retrait ou coordination logistique possible autour de " + location + ".");
    if (req.getType() == ListingType.GROUP_BUYING) {
      points.add("Achat groupe pense pour reduire le cout unitaire et securiser le volume.");
    }
    return points;
  }

  private List<String> buildTags(
      ListingMarketingRequest req, String product, String category, String location) {
    Set<String> tags = new LinkedHashSet<>();
    tags.add(slug(category));
    tags.add(slug(product));
    tags.add(req.getType().name().toLowerCase(Locale.ROOT));
    tags.add(slug(location));
    tags.add("b2b");
    tags.add("eco-ressource");
    return tags.stream().filter(s -> !s.isBlank()).limit(8).toList();
  }

  private String callToAction(ListingType type) {
    if (type == ListingType.DEMANDE) {
      return "Contactez-nous avec votre disponibilite, delai et conditions de livraison.";
    }
    if (type == ListingType.GROUP_BUYING) {
      return "Rejoignez le groupe avant la date limite pour reserver votre quantite.";
    }
    return "Contactez le vendeur pour reserver le lot ou demander une visite.";
  }

  private int score(ListingMarketingRequest req) {
    int score = 45;
    if (req.getTitle() != null && req.getTitle().length() >= 12) score += 10;
    if (req.getDescription() != null && req.getDescription().length() >= 120) score += 15;
    if (req.getProductCategory() != null && !req.getProductCategory().isBlank()) score += 10;
    if (req.getLocation() != null && !req.getLocation().isBlank()) score += 10;
    if (req.getPrice() != null && req.getPrice() > 0) score += 10;
    return Math.min(score, 100);
  }

  private String detectMaterial(String text) {
    String s = clean(text).toLowerCase(Locale.ROOT);
    if (s.matches(".*(aluminium|alu).*")) return "Aluminium";
    if (s.matches(".*(fer|acier|metal|métal).*")) return "Metal";
    if (s.matches(".*(plastique|pet|pvc|poly).*")) return "Plastique";
    if (s.matches(".*(carton|papier).*")) return "Papier/Carton";
    if (s.matches(".*(bois|palette).*")) return "Bois";
    if (s.matches(".*(verre|glass).*")) return "Verre";
    if (s.matches(".*(textile|tissu).*")) return "Textile";
    return "Matiere recyclable";
  }

  private double suggestLocalPrice(ListingMarketingRequest req, String materialType) {
    double base =
        switch (materialType) {
          case "Aluminium" -> 5.2;
          case "Metal" -> 2.4;
          case "Plastique" -> 1.4;
          case "Papier/Carton" -> 0.45;
          case "Bois" -> 0.8;
          case "Verre" -> 0.35;
          case "Textile" -> 0.6;
          default -> 1.0;
        };
    if (req.getType() == ListingType.DEMANDE) {
      base *= 1.08;
    }
    if (req.getType() == ListingType.GROUP_BUYING) {
      base *= 0.93;
    }
    return Math.round(base * 100.0) / 100.0;
  }

  private String typeLabel(ListingType type) {
    return switch (type) {
      case DEMANDE -> "Recherche";
      case GROUP_BUYING -> "Achat groupe";
      case SURPLUS -> "Lot disponible";
    };
  }

  private String firstNonBlank(String first, String fallback) {
    return first == null || first.isBlank() ? fallback : first;
  }

  private String clean(String value) {
    return value == null ? "" : value.trim().replaceAll("\\s+", " ");
  }

  private String trimTo(String value, int max) {
    return value.length() <= max ? value : value.substring(0, max - 1).trim();
  }

  private String titleCase(String value) {
    String[] parts = clean(value).split(" ");
    List<String> out = new ArrayList<>();
    for (String part : parts) {
      if (part.isBlank()) continue;
      out.add(part.substring(0, 1).toUpperCase(Locale.ROOT) + part.substring(1));
    }
    return String.join(" ", out);
  }

  private String slug(String value) {
    String normalized = Normalizer.normalize(clean(value), Normalizer.Form.NFD);
    return normalized
        .replaceAll("\\p{M}", "")
        .toLowerCase(Locale.ROOT)
        .replaceAll("[^a-z0-9]+", "-")
        .replaceAll("(^-|-$)", "");
  }
}
