package com.nqt.rating_service.service.impl;

import com.nqt.rating_service.dto.response.RatingSummaryResponse;
import java.util.UUID;

public interface IRatingSummaryService {
    RatingSummaryResponse getExpertSummary(UUID expertId);

    RatingSummaryResponse recalculateExpertSummary(UUID expertId);
}
