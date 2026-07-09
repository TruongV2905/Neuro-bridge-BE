package com.nqt.payment_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.nqt.payment_service.constant.EscrowStatus;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProjectEscrowResponse {
    UUID id;

    UUID projectId;

    String clientCode;

    String expertCode;

    UUID jobPostId;
    BigDecimal amount;
    BigDecimal heldAmount;
    BigDecimal platformFee;


    LocalDateTime createdAt;
    EscrowStatus status;
}
