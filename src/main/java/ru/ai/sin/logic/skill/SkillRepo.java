package ru.ai.sin.logic.skill;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.Optional;
import java.util.Set;

@Repository
public interface SkillRepo extends JpaRepository<SkillEnt, Long>, JpaSpecificationExecutor<SkillEnt> {

    Set<SkillEnt> findAllByIdIn(Collection<Long> ids);
    Optional<SkillEnt> findByNameIgnoreCase(String name);

    @NonNull
    Page<SkillEnt> findAll(Specification<SkillEnt> spec, @NonNull Pageable pageable);
}
