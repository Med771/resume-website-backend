package ru.ai.sin.logic.user;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.RoleEnum;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepo extends JpaRepository<UserEnt, UUID>, JpaSpecificationExecutor<UserEnt> {

    boolean existsByUsername(String username);

    Optional<UserEnt> findByUsername(String username);

    @Query("SELECT u FROM UserEnt u LEFT JOIN FETCH u.recruiter WHERE u.username = :username")
    Optional<UserEnt> findByUsernameFetchingRecruiter(@Param("username") String username);

    @Query("SELECT DISTINCT u FROM UserEnt u LEFT JOIN FETCH u.recruiter LEFT JOIN FETCH u.student WHERE u.username = :username")
    Optional<UserEnt> findByUsernameFetchingLinks(@Param("username") String username);

    Optional<UserEnt> findByRecruiter_Id(UUID recruiterId);

    Optional<UserEnt> findByStudent_Id(UUID studentId);

    @Query("select u.role, count(u) from UserEnt u group by u.role")
    List<Object[]> countAllGroupedByRole();

    Page<UserEnt> findByAccountStatus(AccountStatus accountStatus, Pageable pageable);

    Page<UserEnt> findByAccountStatusAndRole(AccountStatus accountStatus, RoleEnum role, Pageable pageable);
}
