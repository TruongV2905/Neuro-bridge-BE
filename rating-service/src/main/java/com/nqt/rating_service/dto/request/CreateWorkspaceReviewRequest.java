package com.nqt.rating_service.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CreateWorkspaceReviewRequest {
    // TODO: Derive expertId from WorkspaceRatingClient once workspace-service gRPC is ready.
    // This temporary field must not be trusted in the final integrated flow.
    @NotNull(message = "FIELD_REQUIRED")
    UUID expertId;

    @NotNull(message = "FIELD_REQUIRED")
    @DecimalMin(value = "0.0", inclusive = true, message = "INVALID_FORMAT")
    @DecimalMax(value = "5.0", inclusive = true, message = "INVALID_FORMAT")
    BigDecimal ratingScore;

    @Size(max = 1000, message = "COMMENT_TOO_LONG")
    String comment;
}
