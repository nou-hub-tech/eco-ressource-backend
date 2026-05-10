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

    c.setAllowedOriginPatterns(List.of("*"));
    c.setAllowCredentials(false);
    c.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    c.setAllowedHeaders(List.of("*"));
    c.setExposedHeaders(
            List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, "Content-Disposition")
    );
    c.setMaxAge(3600L);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", c);

    return source;
  }
}