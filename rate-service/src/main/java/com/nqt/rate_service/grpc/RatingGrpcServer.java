package com.nqt.rate_service.grpc;

import com.nqt.rate_service.dto.response.ExpertRatingSummaryResponse;
import com.nqt.rate_service.service.IRatingService;
import com.rating.grpc.ExpertRatingRequest;
import com.rating.grpc.ExpertRatingSummary;
import com.rating.grpc.RatingServiceGrpc;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class RatingGrpcServer extends RatingServiceGrpc.RatingServiceImplBase {

    private final IRatingService ratingService;

    @Override
    public void getExpertRatingSummary(ExpertRatingRequest request,
                                       StreamObserver<ExpertRatingSummary> responseObserver) {
        ExpertRatingSummaryResponse summary = ratingService.getExpertRatingSummary(
                UUID.fromString(request.getExpertId())
        );

        responseObserver.onNext(
                ExpertRatingSummary.newBuilder()
                        .setAverageScore(summary.getAverageScore())
                        .setTotalRatings(summary.getTotalRatings().intValue())
                        .build()
        );
        responseObserver.onCompleted();
    }
}