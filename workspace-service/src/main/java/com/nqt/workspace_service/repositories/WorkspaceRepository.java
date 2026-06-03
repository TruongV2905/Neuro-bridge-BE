package com.nqt.workspace_service.repositories;

import com.nqt.workspace_service.entities.Workspace;
import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, UUID> {
    boolean existsByJobId(UUID jobId);

    @Query("SELECT w FROM Workspace w WHERE w.client.userId = :userId OR w.expert.userId = :userId")
    Page<Workspace> findByUserId(@Param("userId") UUID userId, Pageable pageable);
}
