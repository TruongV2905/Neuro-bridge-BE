package com.nqt.workspace_service.repository;
import com.nqt.workspace_service.entity.ProjectDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ProjectDetailRepository extends JpaRepository<ProjectDetail, UUID> {
    @Query("SELECT pd FROM ProjectDetail pd WHERE pd.workspace.workspaceId = :workspaceId")
    Optional<ProjectDetail> findProjectDetailByWorkspaceId(@Param("workspaceId") UUID workspaceId);
}
