package com.nqt.notification_service.service;

import com.nqt.notification_service.dto.EmailRequest;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;
    private final TemplateEngine templateEngine;

    @Value("${app.notification.from-email}")
    private String fromEmail;

    @Value("${app.notification.from-name}")
    private String fromName;

    public void sendSimpleEmail(EmailRequest request) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(request.getTo());
            message.setSubject(request.getSubject());
            message.setText(request.getBody());
            
            mailSender.send(message);
            log.info("Simple email sent successfully to: {}", request.getTo());
        } catch (Exception e) {
            log.error("Failed to send simple email to: {}", request.getTo(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }

    public void sendHtmlEmail(EmailRequest request) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");
            
            helper.setFrom(fromEmail, fromName);
            helper.setTo(request.getTo());
            helper.setSubject(request.getSubject());
            
            String htmlContent;
            if (request.getTemplateName() != null && request.getTemplateData() != null) {
                Context context = new Context();
                request.getTemplateData().forEach(context::setVariable);
                htmlContent = templateEngine.process(request.getTemplateName(), context);
            } else {
                htmlContent = request.getBody();
            }
            
            helper.setText(htmlContent, true);
            
            mailSender.send(mimeMessage);
            log.info("HTML email sent successfully to: {}", request.getTo());
        } catch (Exception e) {
            log.error("Failed to send HTML email to: {}", request.getTo(), e);
            throw new RuntimeException("Failed to send email", e);
        }
    }
}
