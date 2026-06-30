package com.nqt.identity_service.repository;

import com.nqt.identity_service.entity.Portfolio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;
@Repository
public interface PortfolioRepository extends JpaRepository<Portfolio, UUID> {
}
