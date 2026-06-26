package com.nqt.jobPost_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApplyJobPostResponse {
    ExpertApplyResponse expertApply;
    ProposalResponse proposalResponse;
    ProposalUnlockResponse proposalUnlock;
}
