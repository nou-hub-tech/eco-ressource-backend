package com.marketplace.backend.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class DiscordBotService {

  @Value("${discord.bot.token:}")
  private String botToken;

  @Value("${discord.bot.channel-id:1498826745200251053}")
  private String targetChannelId;

  private volatile JDA jda;

  @PostConstruct
  public void init() {
    if (botToken == null || botToken.trim().isEmpty()) {
      System.err.println("CRITICAL: Discord bot token is missing. Discord notifications will be disabled.");
      log.warn("Discord bot token is missing. Discord notifications will be disabled.");
      return;
    }

    try {
      System.out.println("Initializing Discord Bot...");
      // build() starts the connection in the background but returns the JDA instance immediately
      jda = JDABuilder.createDefault(botToken).build();
      
      // Wait for ready in the background to avoid blocking Spring startup
      CompletableFuture.runAsync(() -> {
        try {
          jda.awaitReady();
          System.out.println("Discord Bot initialized and ready.");
          log.info("Discord Bot initialized successfully.");
          
          // Send startup notification
          sendNotification("🚀 **Eco-Ressource Backend** has started and the Discord Bot is online!");
          
        } catch (Exception e) {
          System.err.println("ERROR: Failed to wait for Discord Bot to be ready: " + e.getMessage());
          e.printStackTrace();
        }
      });

    } catch (Exception e) {
      System.err.println("ERROR: Failed to start Discord Bot (check token validity): " + e.getMessage());
      e.printStackTrace();
      log.error("Failed to start Discord Bot: {}", e.getMessage());
    }
  }


  public void sendNotification(String message) {
    if (jda == null) {
      System.err.println("ERROR: Cannot send Discord message - Bot is not initialized (check token).");
      log.warn("Skipped sending Discord message because bot is not initialized.");
      return;
    }

    CompletableFuture.runAsync(() -> {
      try {
        // Ensure bot is ready before sending
        if (jda.getStatus() != JDA.Status.CONNECTED && jda.getStatus() != JDA.Status.LOADING_SUBSYSTEMS) {
          System.out.println("Waiting for Discord Bot to connect...");
          jda.awaitReady();
        }

        TextChannel channel = jda.getTextChannelById(targetChannelId);
        if (channel != null) {
          channel.sendMessage(message).queue(
              success -> System.out.println("Discord message sent successfully: " + message),
              error -> {
                System.err.println("ERROR: Failed to send Discord message: " + error.getMessage());
                error.printStackTrace();
              });
        } else {
          System.err.println("ERROR: Discord TextChannel with ID " + targetChannelId + " not found.");
          log.warn("Discord TextChannel with ID {} not found.", targetChannelId);
        }
      } catch (Exception e) {
        System.err.println("ERROR: Exception while sending message to Discord: " + e.getMessage());
        e.printStackTrace();
        log.error("Failed to send message to Discord: {}", e.getMessage());
      }
    });
  }
}
