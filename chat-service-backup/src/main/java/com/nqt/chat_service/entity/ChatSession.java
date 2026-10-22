package com.nqt.chat_service.entity;

import com.nqt.chat_service.constant.ChatSessionStatus;
import com.nqt.chat_service.constant.ChatType;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UuidGenerator;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Builder
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ChatSession {
    @Id
    @UuidGenerator
    UUID id;

    UUID workspaceId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ChatSessionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    ChatType type;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    LocalDateTime createdAt;

    @OneToMany(
            mappedBy = "chatSession",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    Set<Messages> messages = new HashSet<>();

    @ElementCollection
    @CollectionTable(
            name = "chat_session_participants",
            joinColumns = @JoinColumn(name = "chat_session_id")
    )
    @Column(name = "user_code", nullable = false)
    @Builder.Default
    Set<String> participantCodes = new HashSet<>();
}
