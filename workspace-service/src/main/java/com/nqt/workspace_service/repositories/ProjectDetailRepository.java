package com.nqt.workspace_service.repositories;
import com.nqt.workspace_service.entities.ProjectDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProjectDetailRepository extends JpaRepository<ProjectDetail, UUID> {
}
