package com.nqt.chat_service.entity;

import com.nqt.chat_service.constant.SenderRole;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Messages {
    @Id
    @UuidGenerator
    UUID id;

    UUID senderId;

    @Enumerated(EnumType.STRING)
    SenderRole senderRole;

    @Column(columnDefinition = "TEXT")
    String content;

    @Column(nullable = false)
    @CreationTimestamp
    LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_session_id", nullable = false)
    ChatSession chatSession;

}
