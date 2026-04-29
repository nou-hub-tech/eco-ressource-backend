package com.marketplace.backend.service;

import com.marketplace.backend.entity.EventParticipation;
import com.marketplace.backend.entity.PlatformEvent;
import com.marketplace.backend.repository.EventParticipationRepository;
import com.marketplace.backend.repository.PlatformEventRepository;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EventReminderScheduler {

  private final PlatformEventRepository platformEventRepository;
  private final EventParticipationRepository eventParticipationRepository;
  private final EmailService emailService;

  private final Set<Long> notifiedEventIds = new HashSet<>();

  @Scheduled(fixedRate = 300000)
  public void checkAndSendReminders() {
    LocalDate tomorrow = LocalDate.now().plusDays(1);
    log.info("Checking events for tomorrow: {}", tomorrow);

    List<PlatformEvent> tomorrowEvents = platformEventRepository.findByEventDate(tomorrow);

    for (PlatformEvent event : tomorrowEvents) {
      if (notifiedEventIds.contains(event.getId())) {
        continue;
      }

      List<EventParticipation> participations =
          eventParticipationRepository.findByPlatformEventId(event.getId());

      if (participations.isEmpty()) {
        log.info("No participants for event '{}', skipping.", event.getTitle());
        notifiedEventIds.add(event.getId());
        continue;
      }

      for (EventParticipation participation : participations) {
        String email = participation.getUser().getEmail();
        String userName = participation.getUser().getFullName();
        String subject = "\uD83D\uDCC5 Reminder: " + event.getTitle() + " is tomorrow!";
        String htmlBody = buildReminderEmail(event, userName);

        emailService.sendHtmlEmail(email, subject, htmlBody);
      }

      notifiedEventIds.add(event.getId());
      log.info("Reminders sent for event '{}' to {} participants.",
          event.getTitle(), participations.size());
    }
  }

  private String buildReminderEmail(PlatformEvent event, String userName) {
    String dateFormatted = event.getEventDate().format(DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy"));
    String description = event.getDescription() != null ? event.getDescription() : "";

    return """
        <!DOCTYPE html>
        <html>
        <head>
          <meta charset="UTF-8">
          <meta name="viewport" content="width=device-width, initial-scale=1.0">
        </head>
        <body style="margin:0;padding:0;font-family:'Segoe UI',Tahoma,Geneva,Verdana,sans-serif;background-color:#f4f7fa;">
          <table width="100%%" cellpadding="0" cellspacing="0" style="max-width:600px;margin:0 auto;background:#ffffff;border-radius:16px;overflow:hidden;box-shadow:0 4px 24px rgba(0,0,0,0.08);">
            <tr>
              <td style="background:linear-gradient(135deg,#6366f1,#8b5cf6);padding:40px 32px;text-align:center;">
                <h1 style="margin:0;color:#ffffff;font-size:28px;font-weight:700;">📅 Event Reminder</h1>
                <p style="margin:8px 0 0;color:rgba(255,255,255,0.85);font-size:15px;">Don't forget — your event is tomorrow!</p>
              </td>
            </tr>
            <tr>
              <td style="padding:32px;">
                <p style="margin:0 0 20px;font-size:16px;color:#334155;">
                  Hi <strong>%s</strong>,
                </p>
                <p style="margin:0 0 24px;font-size:15px;color:#475569;line-height:1.6;">
                  This is a friendly reminder that you're registered for an upcoming event. Here are the details:
                </p>
                <table width="100%%" cellpadding="0" cellspacing="0" style="background:#f8fafc;border-radius:12px;border:1px solid #e2e8f0;">
                  <tr>
                    <td style="padding:24px;">
                      <h2 style="margin:0 0 16px;font-size:20px;color:#1e293b;">%s</h2>
                      <table cellpadding="0" cellspacing="0" style="width:100%%;">
                        <tr>
                          <td style="padding:8px 0;font-size:14px;color:#64748b;width:36px;vertical-align:top;">📅</td>
                          <td style="padding:8px 0;font-size:14px;color:#334155;"><strong>Date:</strong> %s</td>
                        </tr>
                        <tr>
                          <td style="padding:8px 0;font-size:14px;color:#64748b;vertical-align:top;">📍</td>
                          <td style="padding:8px 0;font-size:14px;color:#334155;"><strong>Location:</strong> %s</td>
                        </tr>
                        <tr>
                          <td style="padding:8px 0;font-size:14px;color:#64748b;vertical-align:top;">🏷️</td>
                          <td style="padding:8px 0;font-size:14px;color:#334155;"><strong>Type:</strong> %s</td>
                        </tr>
                        <tr>
                          <td style="padding:8px 0;font-size:14px;color:#64748b;vertical-align:top;">👥</td>
                          <td style="padding:8px 0;font-size:14px;color:#334155;"><strong>Participants:</strong> %d</td>
                        </tr>
                      </table>
                      %s
                    </td>
                  </tr>
                </table>
                <p style="margin:24px 0 0;font-size:14px;color:#94a3b8;text-align:center;">
                  We look forward to seeing you there! 🎉
                </p>
              </td>
            </tr>
            <tr>
              <td style="background:#f8fafc;padding:20px 32px;text-align:center;border-top:1px solid #e2e8f0;">
                <p style="margin:0;font-size:12px;color:#94a3b8;">
                  EcoRessource Marketplace — B2B Circular Economy Platform
                </p>
              </td>
            </tr>
          </table>
        </body>
        </html>
        """.formatted(
        userName,
        event.getTitle(),
        dateFormatted,
        event.getLocation(),
        event.getTypeLabel(),
        event.getParticipants(),
        description.isEmpty() ? "" :
            "<div style=\"margin-top:16px;padding-top:16px;border-top:1px solid #e2e8f0;\">" +
            "<p style=\"margin:0;font-size:14px;color:#64748b;\"><strong>Description:</strong></p>" +
            "<p style=\"margin:8px 0 0;font-size:14px;color:#475569;line-height:1.5;\">" + description + "</p></div>"
    );
  }
}
