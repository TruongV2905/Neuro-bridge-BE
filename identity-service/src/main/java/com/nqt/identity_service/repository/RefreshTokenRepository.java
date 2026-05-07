package com.nqt.identity_service.repository;

import com.nqt.identity_service.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.List;


@Repository
public interface RefreshTokenRepository extends CrudRepository<RefreshToken, String> {

    List<RefreshToken> findAllByUserId(String userId);
}
