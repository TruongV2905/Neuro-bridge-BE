package com.nqt.workspace_service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class WorkspaceDocumentResponse {

    // Document do expert viết, null nếu chưa cập nhật lần nào
    private String document;

    // true nếu expert chưa cập nhật document -> FE hiện modal yêu cầu cập nhật
    private boolean documentRequired;
}