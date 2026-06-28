package com.nqt.rating_service.integration.workspace;

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
public class WorkspaceRatingEligibility {
    UUID workspaceId;
    UUID reviewerId;
    UUID expertId;
    boolean eligible;
    String reason;
}
