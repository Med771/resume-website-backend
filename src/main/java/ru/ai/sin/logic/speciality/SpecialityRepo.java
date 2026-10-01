package ru.ai.sin.logic.speciality;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

@Repository
public interface SpecialityRepo extends JpaRepository<SpecialityEnt, Long>, JpaSpecificationExecutor<SpecialityEnt> {

    @NonNull
    Page<SpecialityEnt> findAll(Specification<SpecialityEnt> spec, @NonNull Pageable pageable);
}
