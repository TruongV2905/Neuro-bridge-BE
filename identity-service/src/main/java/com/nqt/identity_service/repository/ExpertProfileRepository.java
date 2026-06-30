package com.nqt.identity_service.repository;

import com.nqt.identity_service.entity.ExpertProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExpertProfileRepository extends JpaRepository<ExpertProfile, UUID> {

}
