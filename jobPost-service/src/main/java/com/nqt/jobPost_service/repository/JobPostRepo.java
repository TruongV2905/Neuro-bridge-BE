package com.nqt.jobPost_service.repository;

import com.nqt.jobPost_service.entity.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface JobPostRepo extends JpaRepository<JobPost, UUID>, JpaSpecificationExecutor<JobPost> {
    List<JobPost> findDistinctByExpertAppliesExpertCode(String expertCode);

}
