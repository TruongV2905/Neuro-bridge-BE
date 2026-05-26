package com.nqt.notification_service.listener;

import com.nqt.notification_service.dto.EmailRequest;
import com.nqt.notification_service.dto.NotificationEvent;
import com.nqt.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationListener {

    private final EmailService emailService;

    @KafkaListener(topics = "notification-events", groupId = "notification-service-group")
    public void handleNotificationEvent(NotificationEvent event) {
        log.info("Received notification event: {}", event);
        
        try {
            EmailRequest emailRequest = EmailRequest.builder()
                    .to(event.getRecipient())
                    .subject(event.getSubject())
                    .body(event.getMessage())
                    .isHtml(false)
                    .build();
            
            emailService.sendSimpleEmail(emailRequest);
            log.info("Notification processed successfully for: {}", event.getRecipient());
        } catch (Exception e) {
            log.error("Failed to process notification event", e);
        }
    }
}
