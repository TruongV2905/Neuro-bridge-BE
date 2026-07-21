package com.nqt.rate_service.service;

import com.nqt.common_starter.constant.ErrorCode;
import com.nqt.common_starter.exception.GlobalException;
import com.nqt.rate_service.dto.request.RatingRequest;
import com.nqt.rate_service.dto.response.ExpertRatingSummaryResponse;
import com.nqt.rate_service.entity.Rating;
import com.nqt.rate_service.grpc.WorkspaceServiceClient;
import com.nqt.rate_service.repository.RatingRepository;
import com.workspace.grpc.ProjectResponse;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RatingService implements IRatingService {

    private final RatingRepository ratingRepository;
    private final WorkspaceServiceClient workspaceServiceClient;

    private static final String COMPLETED_STATUS = "COMPLETED"; // khớp ProjectState.COMPLETED

    @Override
    @Transactional
    public Rating rateProject(UUID projectId, String currentUserId, RatingRequest request) {

        ProjectResponse project = workspaceServiceClient.getProjectById(projectId.toString());

        if (!COMPLETED_STATUS.equalsIgnoreCase(project.getStatus())) {
            throw new GlobalException(
                    ErrorCode.INVALID_CODE.getCode(),
                    "Dự án chưa hoàn thành, không thể đánh giá"
            );
        }

        // clientId trong ProjectResponse là UUID thật, so sánh trực tiếp với currentUserId từ JWT
        if (!project.getClientId().equals(currentUserId)) {
            throw new GlobalException(
                    ErrorCode.UNAUTHORIZED.getCode(),
                    "Chỉ Client của dự án mới được đánh giá Expert"
            );
        }

        if (ratingRepository.existsByProjectId(projectId)) {
            throw new GlobalException(
                    ErrorCode.INVALID_CODE.getCode(),
                    "Dự án này đã được đánh giá rồi"
            );
        }

        Rating rating = Rating.builder()
                .projectId(projectId)
                .clientId(UUID.fromString(project.getClientId()))
                .expertId(UUID.fromString(project.getExpertId()))
                .score(request.getScore())
                .comment(request.getComment())
                .build();

        return ratingRepository.save(rating);
    }

    @Override
    public ExpertRatingSummaryResponse getExpertRatingSummary(UUID expertId) {
        Double avg = ratingRepository.findAverageScoreByExpertId(expertId);
        long total = ratingRepository.countByExpertId(expertId);

        return ExpertRatingSummaryResponse.builder()
                .expertId(expertId)
                .averageScore(avg != null ? Math.round(avg * 10) / 10.0 : 0.0)
                .totalRatings(total)
                .build();
    }

    @Override
    public Rating getByProjectId(UUID projectId) {
        return ratingRepository.findByProjectId(projectId)
                .orElseThrow(() -> new GlobalException(
                        ErrorCode.NOT_FOUND.getCode(),
                        "Rating " + ErrorCode.NOT_FOUND.getMessage()
                ));
    }
}