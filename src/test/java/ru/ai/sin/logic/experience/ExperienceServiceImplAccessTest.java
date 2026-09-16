package ru.ai.sin.logic.experience;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.exception.models.ForbiddenException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.company.CompanyEnt;
import ru.ai.sin.logic.experience.dto.AddExperienceReq;
import ru.ai.sin.logic.experience.dto.ExperienceDTO;
import ru.ai.sin.logic.experience.dto.ExperienceRes;
import ru.ai.sin.logic.experience.dto.UpdateExperienceReq;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.tools.CompanyTools;
import ru.ai.sin.tools.ExperienceTools;
import ru.ai.sin.tools.StudentTools;

import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperienceServiceImplAccessTest {

    private static final UUID OWN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

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

    @InjectMocks
    private ExperienceServiceImpl experienceService;

    @Test
    void create_studentIgnoresForeignStudentId() {
        AddExperienceReq req = new AddExperienceReq(
                1L, OTHER_ID, "Dev", null, LocalDate.of(2020, 1, 1), null);
        when(accountAccessHelper.resolveStudentIdForResumeMutation(OTHER_ID)).thenReturn(OWN_ID);

        ExperienceEnt ent = new ExperienceEnt();
        when(experienceMapper.toEntity(req)).thenReturn(ent);
        when(companyTools.getCompanyOrThrow(1L)).thenReturn(new CompanyEnt());
        StudentEnt own = student(OWN_ID);
        when(studentTools.getStudentOrThrow(OWN_ID)).thenReturn(own);
        when(experienceRepo.save(ent)).thenReturn(ent);
        ExperienceDTO dto = dto(OWN_ID);
        when(experienceTools.mapToDTO(ent)).thenReturn(dto);
        when(securityHelper.getCurrentUsername()).thenReturn("stu");

        assertThat(experienceService.create(req).studentId()).isEqualTo(OWN_ID);
        verify(accountAccessHelper).requireStudentCanMutateResume(OWN_ID);
        verify(studentTools).getStudentOrThrow(OWN_ID);
        verify(studentTools, never()).getStudentOrThrow(OTHER_ID);
    }

    @Test
    void update_foreignRecordForbidden() {
        ExperienceEnt ent = experienceOn();
        when(experienceTools.getExperienceOrThrow(5L)).thenReturn(ent);
        doThrow(new ForbiddenException("Нет доступа к профилю студента"))
                .when(accountAccessHelper).requireStudentCanMutateResume(OTHER_ID);

        UpdateExperienceReq req = new UpdateExperienceReq(
                1L, OWN_ID, "Dev", null, LocalDate.of(2020, 1, 1), null);

        assertThatThrownBy(() -> experienceService.update(5L, req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void delete_foreignRecordForbidden() {
        when(experienceTools.getExperienceOrThrow(5L)).thenReturn(experienceOn());
        doThrow(new ForbiddenException("Нет доступа к профилю студента"))
                .when(accountAccessHelper).requireStudentCanMutateResume(OTHER_ID);

        assertThatThrownBy(() -> experienceService.deleteById(5L))
                .isInstanceOf(ForbiddenException.class);
        verify(experienceRepo, never()).delete(any(ExperienceEnt.class));
    }

    @Test
    void getById_catalogVisible_ok() {
        when(experienceTools.getExperienceOrThrow(5L)).thenReturn(experienceOn());
        ExperienceDTO dto = dto(OTHER_ID);
        when(experienceTools.mapToDTO(any())).thenReturn(dto);

        assertThat(experienceService.getById(5L)).isEqualTo(dto);
        verify(accountAccessHelper).requireCanReadStudentResumeDetails(OTHER_ID);
    }

    @Test
    void getById_hiddenFromCatalog_notFound() {
        when(experienceTools.getExperienceOrThrow(5L)).thenReturn(experienceOn());
        doThrow(new NotFoundException("Failed to find student by id " + OTHER_ID))
                .when(accountAccessHelper).requireCanReadStudentResumeDetails(OTHER_ID);

        assertThatThrownBy(() -> experienceService.getById(5L))
                .isInstanceOf(NotFoundException.class);
    }

    private static ExperienceDTO dto(UUID studentId) {
        return new ExperienceDTO(
                1L,
                studentId,
                new ExperienceRes(1L, "Dev", null, LocalDate.of(2020, 1, 1), null));
    }

    private static ExperienceEnt experienceOn() {
        ExperienceEnt ent = new ExperienceEnt();
        ent.setStudent(student(ExperienceServiceImplAccessTest.OTHER_ID));
        return ent;
    }

    private static StudentEnt student(UUID id) {
        StudentEnt student = new StudentEnt();
        student.setId(id);
        return student;
    }
}
