package ru.ai.sin.logic.student;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.FileHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.chat.ChatRepo;
import ru.ai.sin.logic.company.CompanyRepo;
import ru.ai.sin.logic.education.EducationRepo;
import ru.ai.sin.logic.experience.ExperienceRepo;
import ru.ai.sin.logic.institution.InstitutionRepo;
import ru.ai.sin.logic.portfolio.PortfolioRepo;
import ru.ai.sin.logic.request.RequestRepo;
import ru.ai.sin.logic.skill.SkillRepo;
import ru.ai.sin.logic.student.dto.StudentDTO;
import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;
import ru.ai.sin.tools.SkillTools;
import ru.ai.sin.tools.SpecialityTools;
import ru.ai.sin.tools.StudentTools;
import ru.ai.sin.tools.UserTools;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
    private SecurityHelper securityHelper;
    @Mock
    private AccountAccessHelper accountAccessHelper;
    @Mock
    private UserTools userTools;

    @InjectMocks
    private StudentServiceImpl studentService;

    @Test
    void getById_hiddenFromCatalog_nonAdmin_notFound() {
        StudentEnt ent = new StudentEnt();
        ent.setId(ID);
        ent.setCatalogVisible(false);
        when(studentTools.getStudentOrThrow(ID)).thenReturn(ent);
        when(securityHelper.isCurrentUserAdmin()).thenReturn(false);

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
        StudentDTO dto = new StudentDTO(
                ID, "c", "h", LocalDate.of(1999, 5, 5), null, null,
                CourseEnum.FIRST, BusynessEnum.FREE, "A", "B", null, null, null, null, null, 1L, "S",
                List.of(new SkillDTO(1L, "x")),
                false,
                false,
                0,
                null
        );
        when(studentTools.getStudentOrThrow(ID)).thenReturn(ent);
        when(securityHelper.isCurrentUserAdmin()).thenReturn(true);
        when(studentTools.mapToDTO(ent)).thenReturn(dto);

        assertThat(studentService.getById(ID)).isEqualTo(dto);
    }
}
