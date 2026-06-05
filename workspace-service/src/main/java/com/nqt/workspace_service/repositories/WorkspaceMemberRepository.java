package com.nqt.workspace_service.repositories;
import com.nqt.workspace_service.entities.WorkspaceMember;
import feign.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, UUID> {
    @Query("SELECT m FROM WorkspaceMember m WHERE m.workspace.workspaceId = :workspaceId AND m.userId = :userId")
    Optional<WorkspaceMember> findByWorkspaceIdAndUserId(@Param("workspaceId") UUID workspaceId, @Param("userId") UUID userId);
    @Query("SELECT m FROM WorkspaceMember m WHERE m.workspace.workspaceId = :workspaceId")
    List<WorkspaceMember> findByWorkspaceId(@Param("workspaceId") UUID workspaceId);
}