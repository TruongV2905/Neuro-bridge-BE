package com.nqt.identity_service.repository;

import com.nqt.identity_service.entity.ClientProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ClientProfileRepository extends JpaRepository<ClientProfile, UUID> {
}
