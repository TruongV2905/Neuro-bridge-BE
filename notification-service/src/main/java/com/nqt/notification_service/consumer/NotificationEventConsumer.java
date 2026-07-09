package com.nqt.notification_service.consumer;

import com.nqt.common_starter.dto.event.NotificationEvent;
import com.nqt.notification_service.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class NotificationEventConsumer {

    private final NotificationService notificationService;

    @KafkaListener(
            topics = {
                    "neural-bridge.package.submitted",
                    "neural-bridge.package.resubmitted",
                    "neural-bridge.package.approved",
                    "neural-bridge.package.rejected",
                    "neural-bridge.workspace.invited",
                    "neural-bridge.jobpost.applied",
                    "neural-bridge.jobpost.accepted",
                    "neural-bridge.contract.sign-required",
                    "neural-bridge.contract.fully-signed"
                    // Thêm topic mới ở đây
            },
            groupId = "notification-service"
    )
    public void consume(NotificationEvent event) {
        log.info("[Neural Bridge] Nhận event={} recipient={}",
                event.getEventType(), event.getRecipientUserId());
        notificationService.sendNotification(event);
    }
}
