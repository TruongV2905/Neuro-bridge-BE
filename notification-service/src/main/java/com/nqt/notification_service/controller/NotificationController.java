package com.nqt.notification_service.controller;

import com.nqt.notification_service.dto.EmailRequest;
import com.nqt.notification_service.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final EmailService emailService;

    @PostMapping("/email/simple")
    public ResponseEntity<String> sendSimpleEmail(@RequestBody EmailRequest request) {
        emailService.sendSimpleEmail(request);
        return ResponseEntity.ok("Email sent successfully");
    }

    @PostMapping("/email/html")
    public ResponseEntity<String> sendHtmlEmail(@RequestBody EmailRequest request) {
        emailService.sendHtmlEmail(request);
        return ResponseEntity.ok("HTML email sent successfully");
    }
//cuoc song thuc vat
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Notification service is running");
    }
}
