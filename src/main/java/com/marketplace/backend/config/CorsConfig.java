package com.marketplace.backend.config;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class CorsConfig {

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration c = new CorsConfiguration();
    /*
     * Patterns (Spring 5.3+) : tout port sur localhost / 127.0.0.1 / ::1.
     * Évite les blocages CORS quand l’app Angular est ouverte en 127.0.0.1:4200
     * alors que seule l’origine http://localhost:4200 était listée.
     * Ne pas mélanger setAllowedOrigins et setAllowedOriginPatterns sur le même objet.
     */
    c.setAllowedOriginPatterns(
        List.of("http://localhost:*", "http://127.0.0.1:*", "http://[::1]:*"));
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("*"));
    c.setExposedHeaders(
        List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, "Content-Disposition"));
    c.setAllowCredentials(true);
    c.setMaxAge(3600L);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);
    return source;
  }
}
