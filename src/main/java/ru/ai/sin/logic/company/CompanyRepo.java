package ru.ai.sin.logic.company;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.lang.NonNull;

import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepo extends JpaRepository<CompanyEnt, Long>, JpaSpecificationExecutor<CompanyEnt> {

    @EntityGraph(attributePaths = {"experiences"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<CompanyEnt> findWithExperiencesById(Long id);

    Optional<CompanyEnt> findFirstByNameIgnoreCase(String name);

    @NonNull
    Page<CompanyEnt> findAll(Specification<CompanyEnt> spec, @NonNull Pageable pageable);
}
