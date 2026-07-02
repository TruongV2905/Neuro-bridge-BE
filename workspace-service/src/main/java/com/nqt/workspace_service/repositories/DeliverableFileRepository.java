package com.nqt.workspace_service.repositories;
import com.nqt.workspace_service.entities.DeliverableFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DeliverableFileRepository extends JpaRepository<DeliverableFile, UUID> {

    @Query("SELECT d FROM DeliverableFile d WHERE d.submissionPackage.packageId = :packageId AND d.isDelete = false")
    List<DeliverableFile> findByPackageIdAndNotDeleted(@Param("packageId") UUID packageId);

    @Query("SELECT p.workspace.workspaceId FROM SubmissionPackage p WHERE p.packageId = :packageId")
    UUID findWorkspaceIdByPackageId(@Param("packageId") UUID packageId);
}
