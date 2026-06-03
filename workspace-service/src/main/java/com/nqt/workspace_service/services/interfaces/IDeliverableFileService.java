package com.nqt.workspace_service.services.interfaces;

import com.nqt.workspace_service.dto.request.AddFileRequest;
import com.nqt.workspace_service.dto.request.UpdateFileRequest;
import com.nqt.workspace_service.dto.response.DeliverableFileRespond;

import java.util.List;
import java.util.UUID;

public interface IDeliverableFileService {
    List<DeliverableFileRespond> getFilesByPackage(UUID packageId, UUID currentUserId);
    DeliverableFileRespond addFile(AddFileRequest req, UUID currentUserId);
    DeliverableFileRespond updateFile(UpdateFileRequest req, UUID currentUserId);
    boolean deleteFile(UUID fileId, UUID currentUserId);
}
