package com.marketplace.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import io.github.cdimascio.dotenv.Dotenv;

@SpringBootApplication
@EnableAsync

public class MarketplaceApplication {

  public static void main(String[] args) {
    // Load .env file (checking root and subfolder)
    Dotenv dotenv = Dotenv.configure()
        .ignoreIfMissing()
        .load();

    dotenv.entries().forEach(entry -> {
      if (System.getenv(entry.getKey()) == null) {
        System.setProperty(entry.getKey(), entry.getValue());
      }
    });

    // Debug: Check if tokens are resolved
    checkToken("DISCORD_BOT_TOKEN", "Discord");
    checkToken("OPENROUTER_API_KEY", "OpenRouter");

    SpringApplication.run(MarketplaceApplication.class, args);
  }

  private static void checkToken(String key, String label) {
    String value = System.getProperty(key);
    if (value == null)
      value = System.getenv(key);

    if (value != null && !value.isEmpty()) {
      System.out.println("✅ " + label + " token loaded successfully (Starts with: " + value.substring(0, 4) + "...)");
    } else {
      System.err.println("❌ " + label + " token NOT found!");
    }
  }
}
