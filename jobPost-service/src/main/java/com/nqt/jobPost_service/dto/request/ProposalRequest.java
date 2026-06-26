package com.nqt.jobPost_service.dto.request;

import com.nqt.jobPost_service.entity.ProposalUnlock;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProposalRequest {
    @NotBlank(message = "Title is required")
    String title;
    @NotBlank(message = "Strategy summary is required")
    String strategySummary;
}
