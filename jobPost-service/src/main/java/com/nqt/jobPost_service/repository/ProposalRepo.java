package com.nqt.jobPost_service.repository;

import com.nqt.jobPost_service.entity.Proposal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.rmi.server.UID;
import java.util.UUID;

public interface ProposalRepo extends JpaRepository<Proposal, UUID> {
}
