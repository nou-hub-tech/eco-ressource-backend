package com.marketplace.backend.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration c = new CorsConfiguration();
    /*
     * Spring rejette l’origine avec 403 si elle n’est pas listée (navigateur envoie
     * Origin: http://127.0.0.1:4200 quand on ouvre l’app sur 127.0.0.1 — pas équivalent à localhost).
     * Le proxy Angular réécrit souvent Origin, mais ne pas dépendre uniquement de ça.
     */
    c.setAllowedOrigins(
        List.of(
            "http://localhost:4200",
            "http://127.0.0.1:4200",
            "http://localhost:9090"));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("*"));
    c.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }
}
