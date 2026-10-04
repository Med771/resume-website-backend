package ru.ai.sin.logic.portfolio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.lang.NonNull;

import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PortfolioRepo extends JpaRepository<PortfolioEnt, Long>, JpaSpecificationExecutor<PortfolioEnt> {

    @NonNull
    @EntityGraph(attributePaths = {"student"}, type = EntityGraph.EntityGraphType.LOAD)
    Optional<PortfolioEnt> findById(@NonNull Long id);

    @NonNull
    @EntityGraph(attributePaths = {"student"}, type = EntityGraph.EntityGraphType.LOAD)
    Page<PortfolioEnt> findAll(Specification<PortfolioEnt> spec, @NonNull Pageable pageable);

    void deleteByStudent_Id(UUID studentId);

    List<PortfolioEnt> findAllByStudent_Id(UUID studentId);
}
