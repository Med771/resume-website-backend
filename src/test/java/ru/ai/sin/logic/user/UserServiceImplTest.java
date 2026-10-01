package ru.ai.sin.logic.user;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.user.dto.AddUserReq;
import ru.ai.sin.logic.user.dto.UserDTO;
import ru.ai.sin.models.enums.RoleEnum;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepo userRepo;
    @Mock
    private StudentRepo studentRepo;
    @Mock
    private UserMapper userMapper;
    @Mock
    private PasswordEncoder passwordEncoder;

    private UserServiceImpl service;

    private final UUID studentId = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @BeforeEach
    void setUp() {
        service = new UserServiceImpl(userRepo, studentRepo, userMapper, passwordEncoder);
    }

    @Test
    void create_studentRoleRequiresStudentId() {
        when(userRepo.existsByUsername("u1")).thenReturn(false);

        AddUserReq req = new AddUserReq("N", "u1", "pw", RoleEnum.STUDENT, null);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("studentId");

        verify(studentRepo, never()).findById(any());
    }

    @Test
    void create_userRoleRejectsStudentId() {
        when(userRepo.existsByUsername("u2")).thenReturn(false);

        AddUserReq req = new AddUserReq("N", "u2", "pw", RoleEnum.RECRUITER, studentId);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("studentId");
    }

    @Test
    void create_studentNotFound() {
        when(userRepo.existsByUsername("u3")).thenReturn(false);
        when(userRepo.findByStudent_Id(studentId)).thenReturn(Optional.empty());
        when(studentRepo.findById(studentId)).thenReturn(Optional.empty());

        AddUserReq req = new AddUserReq("N", "u3", "pw", RoleEnum.STUDENT, studentId);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Student not found");
    }

    @Test
    void create_studentSuccess() {
        when(userRepo.existsByUsername("u4")).thenReturn(false);
        when(userRepo.findByStudent_Id(studentId)).thenReturn(Optional.empty());
        StudentEnt st = new StudentEnt();
        st.setId(studentId);
        when(studentRepo.findById(studentId)).thenReturn(Optional.of(st));
        when(passwordEncoder.encode("pw")).thenReturn("hash");

        when(userRepo.save(any(UserEnt.class))).thenAnswer(inv -> inv.getArgument(0));

        UserDTO dto = new UserDTO(UUID.randomUUID(), "N", "u4", RoleEnum.STUDENT, null, studentId);
        when(userMapper.toDTO(any(UserEnt.class))).thenReturn(dto);

        AddUserReq req = new AddUserReq("N", "u4", "pw", RoleEnum.STUDENT, studentId);

        UserDTO out = service.create(req);

        org.assertj.core.api.Assertions.assertThat(out.studentId()).isEqualTo(studentId);
    }

    @Test
    void create_rejectsAdminRole() {
        when(userRepo.existsByUsername("admin1")).thenReturn(false);

        AddUserReq req = new AddUserReq("N", "admin1", "pw", RoleEnum.ADMIN, null);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("RECRUITER или STUDENT");

        verify(userRepo, never()).save(any());
    }

    @Test
    void create_recruiterRole_succeeds() {
        when(userRepo.existsByUsername("rec1")).thenReturn(false);
        when(passwordEncoder.encode("pw")).thenReturn("hash");
        UserEnt saved = new UserEnt(RoleEnum.RECRUITER, "N", "rec1", "hash");
        saved.setId(UUID.randomUUID());
        when(userRepo.save(any())).thenReturn(saved);
        when(userMapper.toDTO(saved)).thenReturn(new UserDTO(saved.getId(), "N", "rec1", RoleEnum.RECRUITER, null, null));

        AddUserReq req = new AddUserReq("N", "rec1", "pw", RoleEnum.RECRUITER, null);
        org.assertj.core.api.Assertions.assertThat(service.create(req).role()).isEqualTo(RoleEnum.RECRUITER);
    }

    @Test
    void deleteById_rejectsAdminAccount() {
        UUID id = UUID.randomUUID();
        UserEnt admin = new UserEnt(RoleEnum.ADMIN, "A", "root", "h");
        admin.setId(id);
        when(userRepo.findById(id)).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.deleteById(id))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("RECRUITER или STUDENT");

        verify(userRepo, never()).delete(eq(admin));
    }
}
