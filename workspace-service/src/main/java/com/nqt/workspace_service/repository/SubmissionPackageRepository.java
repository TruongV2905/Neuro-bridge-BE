package com.nqt.workspace_service.repository;
import com.nqt.workspace_service.entity.SubmissionPackage;
import com.nqt.workspace_service.constant.PackageStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubmissionPackageRepository extends JpaRepository<SubmissionPackage, UUID> {
    @Query("SELECT MAX(sp.version) FROM SubmissionPackage sp WHERE sp.workspace.workspaceId = :workspaceId")
    Optional<Integer> findMaxVersionByWorkspaceId(@Param("workspaceId") UUID workspaceId);

    List<SubmissionPackage> findByStatusAndAutoReleaseAtBefore(PackageStatus status, LocalDateTime now);

    @Query("SELECT sp FROM SubmissionPackage sp WHERE sp.workspace.workspaceId = :workspaceId AND sp.isDelete = false ORDER BY sp.version DESC")
    List<SubmissionPackage> findByWorkspaceIdAndNotDeleted(@Param("workspaceId") UUID workspaceId);

    @Query("SELECT sp FROM SubmissionPackage sp LEFT JOIN FETCH sp.deliverableFiles WHERE sp.packageId = :packageId AND sp.isDelete = false")
    Optional<SubmissionPackage> findByIdAndNotDeleted(@Param("packageId") UUID packageId);
}
