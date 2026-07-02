package com.nqt.identity_service.grpc;

import com.identity.grpc.SkillSuggestionRequest;
import com.identity.grpc.SkillSuggestionResponse;
import com.identity.grpc.SkillSuggestionServiceGrpc;
import com.nqt.identity_service.dto.response.skill.SkillSuggestionResult;
import com.nqt.identity_service.service.SkillService;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.extern.slf4j.Slf4j;
import net.devh.boot.grpc.server.service.GrpcService;


import java.util.List;

@GrpcService
@Slf4j
public class SkillSuggestionGrpcServiceImpl
        extends SkillSuggestionServiceGrpc.SkillSuggestionServiceImplBase {


    private final SkillService skillService;

    public SkillSuggestionGrpcServiceImpl(SkillService skillService) {
        this.skillService = skillService;
    }

    @Override
    public void suggestSkills(
            SkillSuggestionRequest request,
            StreamObserver<SkillSuggestionResponse> responseObserver
    ) {
        try {
            log.info("gRPC suggestSkills called with title={}", request.getTitle());

            com.nqt.identity_service.dto.request.skill.SkillSuggestionRequest dto =
                    new com.nqt.identity_service.dto.request.skill.SkillSuggestionRequest();

            dto.setTitle(request.getTitle());
            dto.setDescription(request.getDescription());

            SkillSuggestionResult result = skillService.suggestSkills(dto);

            SkillSuggestionResponse response =
                    SkillSuggestionResponse.newBuilder()
                            .addAllRequiredSkillIds(
                                    result.getRequiredSkillIds() == null
                                            ? List.of()
                                            : result.getRequiredSkillIds()
                            )
                            .addAllPreferredSkillIds(
                                    result.getPreferredSkillIds() == null
                                            ? List.of()
                                            : result.getPreferredSkillIds()
                            )
                            .build();

            responseObserver.onNext(response);
            responseObserver.onCompleted();

        } catch (Exception e) {
            log.error("Internal error when suggesting skills via gRPC", e);

            responseObserver.onError(
                    Status.INTERNAL
                            .withDescription("Failed to suggest skills")
                            .withCause(e)
                            .asRuntimeException()
            );
        }
    }
}