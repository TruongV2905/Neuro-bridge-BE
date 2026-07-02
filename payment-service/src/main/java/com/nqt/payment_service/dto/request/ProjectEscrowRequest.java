package com.nqt.payment_service.dto.request;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProjectEscrowRequest {
    UUID projectId;
    String clientCode;
    String expertCode;
    UUID jobPostId;
    BigDecimal amount;
}
