package ru.ai.sin.logic.student;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.logic.account.StudentReviewState;
import ru.ai.sin.logic.profile.ProfileCommunicationGateService;
import ru.ai.sin.logic.verification.VerificationOtpMailer;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.FileHelper;
import ru.ai.sin.logic.chat.ChatRepo;
import ru.ai.sin.logic.company.CompanyRepo;
import ru.ai.sin.logic.education.EducationRepo;
import ru.ai.sin.logic.experience.ExperienceRepo;
import ru.ai.sin.logic.institution.InstitutionRepo;
import ru.ai.sin.logic.portfolio.PortfolioRepo;
import ru.ai.sin.logic.request.RequestRepo;
import ru.ai.sin.logic.skill.SkillRepo;
import ru.ai.sin.logic.student.dto.FilterStudentReq;
import ru.ai.sin.logic.student.dto.StudentCardDTO;
import ru.ai.sin.logic.student.dto.StudentDTO;
import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.tools.SkillTools;
import ru.ai.sin.tools.SpecialityTools;
import ru.ai.sin.tools.StudentTools;
import ru.ai.sin.tools.UserTools;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Бизнес-правила видимости карточки (catalog_visible=false скрыта от не-админов).
 */
@ExtendWith(MockitoExtension.class)
class StudentServiceImplVisibilityTest {

    private static final UUID ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");

    @Mock
    private StudentRepo studentRepo;
    @Mock
    private SkillRepo skillRepo;
    @Mock
    private CompanyRepo companyRepo;
    @Mock
    private EducationRepo educationRepo;
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
    private FileHelper fileHelper;
    @Mock
    private AccountAccessHelper accountAccessHelper;
    @Mock
    private UserTools userTools;
    @Mock
    private ProfileCommunicationGateService profileCommunicationGateService;
    @Mock
    private VerificationOtpMailer verificationOtpMailer;
    @Mock
    private StudentReviewState studentReviewState;

    @InjectMocks
    private StudentServiceImpl studentService;

    @Test
    void getById_hiddenFromCatalog_nonAdmin_notFound() {
        doThrow(new NotFoundException("Failed to find student by id " + ID))
                .when(accountAccessHelper).requireCanReadStudentResumeDetails(ID);

        assertThatThrownBy(() -> studentService.getById(ID))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("Failed to find student");
    }

    @Test
    void getById_hiddenFromCatalog_admin_ok() {
        StudentEnt ent = new StudentEnt();
        ent.setId(ID);
        ent.setCatalogVisible(false);
        ent.setCourse(CourseEnum.FIRST);
        StudentDTO dto = sampleDto();
        when(studentTools.getStudentOrThrow(ID)).thenReturn(ent);
        when(studentTools.mapToDTO(ent)).thenReturn(dto);

        assertThat(studentService.getById(ID)).isEqualTo(dto);
    }

    @Test
    void cardsFilter_unapprovedStudent_usesCatalogQuery() {
        StudentEnt own = new StudentEnt();
        own.setId(ID);
        when(accountAccessHelper.requireCurrentUser()).thenReturn(studentUser(AccountStatus.PENDING_APPROVAL, own));
        when(studentRepo.findAll(ArgumentMatchers.<Specification<StudentEnt>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<StudentCardDTO> page = studentService.getAllCardsByFilter(PageRequest.of(0, 20), emptyFilter());

        assertThat(page.data()).isEmpty();
        verify(studentRepo).findAll(ArgumentMatchers.<Specification<StudentEnt>>any(), any(Pageable.class));
        verify(accountAccessHelper, never()).requireApprovedAccount();
    }

    @Test
    void filter_unapprovedStudent_usesCatalogQuery() {
        StudentEnt own = new StudentEnt();
        own.setId(ID);
        when(accountAccessHelper.requireCurrentUser()).thenReturn(studentUser(AccountStatus.REJECTED, own));
        when(studentRepo.findAll(ArgumentMatchers.<Specification<StudentEnt>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        PageResponse<StudentDTO> page = studentService.getAllByFilter(PageRequest.of(0, 20), emptyFilter());

        assertThat(page.data()).isEmpty();
        verify(studentRepo).findAll(ArgumentMatchers.<Specification<StudentEnt>>any(), any(Pageable.class));
    }

    @Test
    void cardsFilter_unapprovedRecruiter_usesCatalogQuery() {
        UserEnt recruiter = new UserEnt();
        recruiter.setRole(RoleEnum.RECRUITER);
        recruiter.setAccountStatus(AccountStatus.PENDING_APPROVAL);
        when(accountAccessHelper.requireCurrentUser()).thenReturn(recruiter);
        when(studentRepo.findAll(ArgumentMatchers.<Specification<StudentEnt>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        studentService.getAllCardsByFilter(PageRequest.of(0, 20), emptyFilter());

        verify(studentRepo).findAll(ArgumentMatchers.<Specification<StudentEnt>>any(), any(Pageable.class));
        verify(accountAccessHelper, never()).requireApprovedAccount();
    }

    @Test
    void cardsFilter_approvedStudent_usesCatalogQuery() {
        StudentEnt own = new StudentEnt();
        own.setId(ID);
        when(accountAccessHelper.requireCurrentUser()).thenReturn(studentUser(AccountStatus.APPROVED, own));
        when(studentRepo.findAll(ArgumentMatchers.<Specification<StudentEnt>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(own)));
        StudentCardDTO card = new StudentCardDTO(ID, null, "A", "B", null, null, CourseEnum.FIRST, "S", List.of(), null);
        when(studentTools.mapToCardDTO(own)).thenReturn(card);

        PageResponse<StudentCardDTO> page = studentService.getAllCardsByFilter(PageRequest.of(0, 20), emptyFilter());

        assertThat(page.data()).containsExactly(card);
        verify(studentRepo).findAll(ArgumentMatchers.<Specification<StudentEnt>>any(), any(Pageable.class));
    }

    private static UserEnt studentUser(AccountStatus status, StudentEnt student) {
        UserEnt user = new UserEnt();
        user.setRole(RoleEnum.STUDENT);
        user.setAccountStatus(status);
        user.setStudent(student);
        return user;
    }

    private static FilterStudentReq emptyFilter() {
        return new FilterStudentReq(null, null, null, null, null, null, null, null, null, null, null, null);
    }

    private static StudentDTO sampleDto() {
        return new StudentDTO(
                ID, "c", "h", LocalDate.of(1999, 5, 5), null, null,
                CourseEnum.FIRST, BusynessEnum.FREE, null, null, "A", "B", null, null, null, null, null, 1L, "S",
                List.of(new SkillDTO(1L, "x")),
                false,
                false,
                0,
                null
        );
    }
}
