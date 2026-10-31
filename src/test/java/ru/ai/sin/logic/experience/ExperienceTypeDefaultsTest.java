package ru.ai.sin.logic.experience;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.account.StudentReviewState;
import ru.ai.sin.logic.company.CompanyEnt;
import ru.ai.sin.logic.experience.dto.AddExperienceReq;
import ru.ai.sin.logic.experience.dto.UpdateExperienceReq;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.models.enums.ExperienceType;
import ru.ai.sin.tools.CompanyTools;
import ru.ai.sin.tools.ExperienceTools;
import ru.ai.sin.tools.StudentTools;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperienceTypeDefaultsTest {

    private static final UUID STUDENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock
    private ExperienceRepo experienceRepo;
    @Mock
    private ExperienceMapper experienceMapper;
    @Mock
    private ExperienceTools experienceTools;
    @Mock
    private CompanyTools companyTools;
    @Mock
    private StudentTools studentTools;
    @Mock
    private SecurityHelper securityHelper;
    @Mock
    private AccountAccessHelper accountAccessHelper;
    @Mock
    private StudentReviewState studentReviewState;

    @InjectMocks
    private ExperienceServiceImpl experienceService;

    private final ExperienceMapper realMapper = Mappers.getMapper(ExperienceMapper.class);

    @Test
    void create_withoutType_setsWork() {
        AddExperienceReq req = request(null);
        ExperienceEnt ent = new ExperienceEnt();
        ent.setExperienceType(null);
        stubCreate(req, ent);

        experienceService.create(req);

        assertThat(ent.getExperienceType()).isEqualTo(ExperienceType.WORK);
        verify(experienceRepo).save(ent);
    }

    @Test
    void create_withProject_keepsProject() {
        AddExperienceReq req = request(ExperienceType.PROJECT);
        ExperienceEnt ent = new ExperienceEnt();
        ent.setExperienceType(ExperienceType.PROJECT);
        stubCreate(req, ent);

        experienceService.create(req);

        assertThat(ent.getExperienceType()).isEqualTo(ExperienceType.PROJECT);
    }

    @Test
    void update_withoutType_keepsProject() {
        ExperienceEnt ent = new ExperienceEnt();
        ent.setExperienceType(ExperienceType.PROJECT);
        ent.setPosition("Старая");

        realMapper.updateEntityFromDto(update(null, "Новая"), ent);

        assertThat(ent.getExperienceType()).isEqualTo(ExperienceType.PROJECT);
        assertThat(ent.getPosition()).isEqualTo("Новая");
    }

    @Test
    void update_withWork_changesType() {
        ExperienceEnt ent = new ExperienceEnt();
        ent.setExperienceType(ExperienceType.PROJECT);

        realMapper.updateEntityFromDto(update(ExperienceType.WORK, "Разработчик"), ent);

        assertThat(ent.getExperienceType()).isEqualTo(ExperienceType.WORK);
    }

    private void stubCreate(AddExperienceReq req, ExperienceEnt ent) {
        when(accountAccessHelper.resolveStudentIdForResumeMutation(STUDENT_ID)).thenReturn(STUDENT_ID);
        when(experienceMapper.toEntity(req)).thenReturn(ent);
        when(companyTools.getCompanyOrThrow(1L)).thenReturn(new CompanyEnt());
        StudentEnt student = new StudentEnt();
        student.setId(STUDENT_ID);
        when(studentTools.getStudentOrThrow(STUDENT_ID)).thenReturn(student);
        when(experienceRepo.save(ent)).thenReturn(ent);
        when(securityHelper.getCurrentUsername()).thenReturn("stu");
    }

    private static AddExperienceReq request(ExperienceType type) {
        return new AddExperienceReq(1L, STUDENT_ID, "Dev", null, LocalDate.of(2020, 1, 1), null, type);
    }

    private static UpdateExperienceReq update(ExperienceType type, String position) {
        return new UpdateExperienceReq(1L, STUDENT_ID, position, null, LocalDate.of(2020, 1, 1), null, type);
    }
}
