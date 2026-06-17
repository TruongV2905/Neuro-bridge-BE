package com.nqt.identity_service.repository;

import com.nqt.identity_service.entity.Skill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SkillRepository extends JpaRepository<Skill, Long> {
    //client lấy skills
    Page<Skill> findBySkillNameContainingIgnoreCaseAndDeletedFalse(
            String skillName,
            Pageable pageable
    );

    //lấy skills bởi Admin
    Page<Skill> findBySkillNameContainingIgnoreCase(
            String skillName,
            Pageable pageable
    );

    boolean existsBySkillNameIgnoreCase(String skillName);

    List<Skill> findByIdInAndDeletedFalse(Collection<Long> ids);
    Optional<Skill> findByIdAndDeletedFalse(Long id);

}
