package com.nqt.common_starter.kafka;

import com.nqt.common_starter.dto.event.NotificationEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class NotificationEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    // Topic constants — chuẩn chung toàn hệ thống
    public static final String PACKAGE_SUBMITTED   = "neural-bridge.package.submitted";
    public static final String PACKAGE_RESUBMITTED = "neural-bridge.package.resubmitted";
    public static final String PACKAGE_APPROVED    = "neural-bridge.package.approved";
    public static final String PACKAGE_REJECTED    = "neural-bridge.package.rejected";
    public static final String WORKSPACE_INVITED   = "neural-bridge.workspace.invited";

    public void publish(String topic, NotificationEvent event) {
        event.setOccurredAt(LocalDateTime.now());
        kafkaTemplate.send(topic, event.getRecipientUserId().toString(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("[Neural Bridge] Kafka publish failed topic={} error={}",
                                topic, ex.getMessage());
                    } else {
                        log.info("[Neural Bridge] Published {} to {}",
                                event.getEventType(), topic);
                    }
                });
    }
}
