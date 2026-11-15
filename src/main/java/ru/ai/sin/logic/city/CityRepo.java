package ru.ai.sin.logic.city;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CityRepo extends JpaRepository<CityEnt, Long>, JpaSpecificationExecutor<CityEnt> {

    Optional<CityEnt> findFirstByNameIgnoreCase(String name);
}
