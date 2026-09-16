package ru.ai.sin.logic.student;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.FileHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.chat.ChatRepo;
import ru.ai.sin.logic.experience.ExperienceRepo;
import ru.ai.sin.logic.institution.InstitutionRepo;
import ru.ai.sin.logic.portfolio.PortfolioRepo;
import ru.ai.sin.logic.registration.RegistrationPasswordPolicy;
import ru.ai.sin.logic.request.RequestRepo;
import ru.ai.sin.logic.skill.SkillRepo;
import ru.ai.sin.logic.student.dto.PatchStudentMeReq;
import ru.ai.sin.logic.student.dto.StudentDTO;
import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.tools.SkillTools;
import ru.ai.sin.tools.SpecialityTools;
import ru.ai.sin.tools.StudentTools;
import ru.ai.sin.tools.UserTools;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplPatchMeTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private StudentRepo studentRepo;
    @Mock
    private SkillRepo skillRepo;
    @Mock
    private PortfolioRepo portfolioRepo;
    @Mock
    private ExperienceRepo experienceRepo;
    @Mock
    private InstitutionRepo institutionRepo;
    @Mock
    private RequestRepo requestRepo;
    @Mock
    private ChatRepo chatRepo;
    @Mock
    private StudentMapper studentMapper;
    @Mock
    private StudentTools studentTools;
    @Mock
    private SpecialityTools specialityTools;
    @Mock
    private SkillTools skillTools;
    @Mock
    private StudentCvAttachmentService studentCvAttachmentService;
    @Mock
    private StudentSkillsMutator studentSkillsMutator;
    @Mock
    private FileHelper fileHelper;
    @Mock
    private SecurityHelper securityHelper;
    @Mock
    private AccountAccessHelper accountAccessHelper;
    @Mock
    private UserTools userTools;
    @Mock
    private UserRepo userRepo;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private RegistrationPasswordPolicy registrationPasswordPolicy;

    @InjectMocks
    private StudentServiceImpl studentService;

    @Test
    void patchMe_writesBioAndSkills() {
        StudentEnt studentEnt = new StudentEnt();
        studentEnt.setId(ID);
        UserEnt user = new UserEnt();
        user.setRole(RoleEnum.STUDENT);
        user.setStudent(studentEnt);

        when(securityHelper.getCurrentUsername()).thenReturn("stu");
        when(userRepo.findByUsernameFetchingLinks("stu")).thenReturn(Optional.of(user));

        StudentDTO dto = new StudentDTO(
                ID, "c", "h", LocalDate.of(1999, 5, 5), "new bio", null,
                CourseEnum.FIRST, BusynessEnum.FREE, "A", "B", null, null, null, 1L, "S",
                List.of(new SkillDTO(1L, "Java")),
                false,
                false,
                0,
                null
        );
        when(studentTools.mapToDTO(studentEnt)).thenReturn(dto);

        PatchStudentMeReq req = new PatchStudentMeReq(
                null, null, null, "new bio", null, null,
                null, null, null, null, null, null,
                List.of(1L), null, null);

        assertThat(studentService.patchMe(req).bio()).isEqualTo("new bio");
        assertThat(studentEnt.getBio()).isEqualTo("new bio");
        verify(studentSkillsMutator).replaceSkills(studentEnt, List.of(1L));
        verify(accountAccessHelper).requireStudentCanMutateResume(ID);
        assertThat(studentEnt.getProfileTextScore()).isGreaterThan(0);
    }
}
