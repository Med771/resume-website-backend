package ru.ai.sin.logic.student;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.FileHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.account.StudentReviewState;
import ru.ai.sin.logic.chat.ChatRepo;
import ru.ai.sin.logic.experience.ExperienceRepo;
import ru.ai.sin.logic.institution.InstitutionRepo;
import ru.ai.sin.logic.portfolio.PortfolioRepo;
import ru.ai.sin.logic.profile.ProfileCommunicationGateService;
import ru.ai.sin.logic.registration.RegistrationPasswordPolicy;
import ru.ai.sin.logic.verification.VerificationOtpMailer;
import ru.ai.sin.logic.request.RequestRepo;
import ru.ai.sin.logic.skill.SkillRepo;
import ru.ai.sin.logic.student.dto.StudentDTO;
import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.logic.vacancy.VacancyApplicationRepo;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;
import ru.ai.sin.tools.SkillTools;
import ru.ai.sin.tools.SpecialityTools;
import ru.ai.sin.tools.StudentTools;
import ru.ai.sin.tools.UserTools;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceImplDeleteTest {

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
    private VacancyApplicationRepo vacancyApplicationRepo;
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
    @Mock
    private ProfileCommunicationGateService profileCommunicationGateService;
    @Mock
    private VerificationOtpMailer verificationOtpMailer;
    @Mock
    private StudentReviewState studentReviewState;

    @InjectMocks
    private StudentServiceImpl studentService;

    @Test
    void deleteById_removesApplicationsBeforeChats_andUserBeforeStudent() {
        StudentEnt studentEnt = new StudentEnt();
        studentEnt.setId(ID);
        UserEnt user = new UserEnt();
        user.setUsername("stu_user");
        user.setStudent(studentEnt);

        StudentDTO snapshot = new StudentDTO(
                ID, "c", "h", LocalDate.of(1999, 5, 5), "bio", null,
                CourseEnum.FIRST, BusynessEnum.FREE, "A", "B", null, null, "a@b.c", null, null, 1L, "S",
                List.of(new SkillDTO(1L, "Java")),
                false,
                true,
                0,
                null
        );

        when(studentTools.getStudentOrThrow(ID)).thenReturn(studentEnt);
        when(studentTools.mapToDTO(studentEnt)).thenReturn(snapshot);
        when(userRepo.findByStudent_Id(ID)).thenReturn(Optional.of(user));
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        studentService.deleteById(ID);

        InOrder order = inOrder(
                vacancyApplicationRepo,
                requestRepo,
                chatRepo,
                experienceRepo,
                institutionRepo,
                portfolioRepo,
                userRepo,
                studentRepo);
        order.verify(vacancyApplicationRepo).deleteByStudentId(ID);
        order.verify(requestRepo).deleteByStudent_Id(ID);
        order.verify(chatRepo).deleteByStudent_Id(ID);
        order.verify(experienceRepo).deleteByStudent_Id(ID);
        order.verify(institutionRepo).deleteByStudent_Id(ID);
        order.verify(portfolioRepo).deleteByStudent_Id(ID);
        order.verify(userRepo).findByStudent_Id(ID);
        order.verify(userRepo).delete(user);
        order.verify(studentRepo).delete(studentEnt);
    }
}
