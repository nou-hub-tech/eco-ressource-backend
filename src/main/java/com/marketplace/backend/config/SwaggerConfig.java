package com.marketplace.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerConfig {

  @Bean
  public OpenAPI customOpenAPI() {
    return new OpenAPI()
        .info(
            new Info()
                .title("Eco-Ressource Marketplace API")
                .version("1.0")
                .description(
                    "API Backend - Modules: Gestion Stock/Produit, Gestion Annonces/Posts, "
                        + "Group Buying, Commentaires, Favoris"))
        .addSecurityItem(new SecurityRequirement().addList("Bearer JWT"))
        .components(
            new Components()
                .addSecuritySchemes(
                    "Bearer JWT",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")));
  }
}
