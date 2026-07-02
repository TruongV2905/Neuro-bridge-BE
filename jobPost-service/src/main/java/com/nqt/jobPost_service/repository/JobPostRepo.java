package com.nqt.jobPost_service.repository;

import com.nqt.jobPost_service.entity.JobPost;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface JobPostRepo extends JpaRepository<JobPost, UUID>, JpaSpecificationExecutor<JobPost> {
}
