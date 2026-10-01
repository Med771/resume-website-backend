package ru.ai.sin.logic.user;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.user.dto.AddUserReq;
import ru.ai.sin.logic.user.dto.FilterUserReq;
import ru.ai.sin.logic.user.dto.UserDTO;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.enums.RoleEnum;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepo userRepo;
    private final StudentRepo studentRepo;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserDTO> getByFilter(Pageable pageable, FilterUserReq filterUserReq) {
        Page<UserEnt> page = userRepo.findAll(
                UserSpecifications.byFilters(filterUserReq),
                pageable
        );

        return new PageResponse<>(
                page.getContent().stream().map(userMapper::toDTO).toList(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional
    public UserDTO create(AddUserReq addUserReq) {
        if (userRepo.existsByUsername(addUserReq.username())) {
            throw new BadRequestException("User already exists: " + addUserReq.username());
        }

        RoleEnum role = addUserReq.role() != null ? addUserReq.role() : RoleEnum.RECRUITER;
        if (role == RoleEnum.ADMIN) {
            throw new BadRequestException("Создание пользователя допустимо только с ролью RECRUITER или STUDENT");
        }

        UserEnt userEnt;
        if (role == RoleEnum.STUDENT) {
            if (addUserReq.studentId() == null) {
                throw new BadRequestException("Для роли STUDENT укажите studentId");
            }
            if (userRepo.findByStudent_Id(addUserReq.studentId()).isPresent()) {
                throw new BadRequestException("Этот студент уже привязан к пользователю");
            }
            StudentEnt student = studentRepo.findById(addUserReq.studentId())
                    .orElseThrow(() -> new NotFoundException("Student not found: " + addUserReq.studentId()));
            userEnt = new UserEnt(
                    RoleEnum.STUDENT,
                    addUserReq.name(),
                    addUserReq.username(),
                    passwordEncoder.encode(addUserReq.password())
            );
            userEnt.setStudent(student);
        } else {
            if (addUserReq.studentId() != null) {
                throw new BadRequestException("Поле studentId допустимо только для роли STUDENT");
            }
            userEnt = new UserEnt(
                    RoleEnum.RECRUITER,
                    addUserReq.name(),
                    addUserReq.username(),
                    passwordEncoder.encode(addUserReq.password())
            );
        }

        try {
            userEnt = userRepo.save(userEnt);
            log.info("Created user: username={} role={} id={}", userEnt.getUsername(), userEnt.getRole(), userEnt.getId());
            return userMapper.toDTO(userEnt);
        } catch (DataIntegrityViolationException ex) {
            log.warn("User already exists: {}", addUserReq.username());
            throw new BadRequestException("User already exists: " + addUserReq.username());
        }
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        UserEnt userEnt = userRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found: " + id));

        if (userEnt.getRole() != RoleEnum.RECRUITER && userEnt.getRole() != RoleEnum.STUDENT) {
            throw new BadRequestException("Удалять можно только пользователей с ролью RECRUITER или STUDENT");
        }

        userRepo.delete(userEnt);
        log.info("Deleted user: {}", id);
    }
}
