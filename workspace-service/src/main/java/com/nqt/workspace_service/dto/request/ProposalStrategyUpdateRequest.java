package com.nqt.workspace_service.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProposalStrategyUpdateRequest {

    @NotBlank(message = "strategyDetail must not be blank")
    String strategyDetail;
}
