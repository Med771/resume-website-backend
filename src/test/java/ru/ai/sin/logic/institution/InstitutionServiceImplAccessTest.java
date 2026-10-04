package ru.ai.sin.logic.institution;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.exception.models.ForbiddenException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.education.EducationEnt;
import ru.ai.sin.logic.institution.dto.AddInstitutionReq;
import ru.ai.sin.logic.institution.dto.InstitutionDTO;
import ru.ai.sin.logic.institution.dto.InstitutionRes;
import ru.ai.sin.logic.institution.dto.UpdateInstitutionReq;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.tools.EducationTools;
import ru.ai.sin.tools.InstitutionTools;
import ru.ai.sin.tools.StudentTools;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstitutionServiceImplAccessTest {

    private static final UUID OWN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private InstitutionRepo institutionRepo;
    @Mock
    private InstitutionMapper institutionMapper;
    @Mock
    private InstitutionTools institutionTools;
    @Mock
    private EducationTools educationTools;
    @Mock
    private StudentTools studentTools;
    @Mock
    private SecurityHelper securityHelper;
    @Mock
    private AccountAccessHelper accountAccessHelper;

    @InjectMocks
    private InstitutionServiceImpl institutionService;

    @Test
    void create_studentIgnoresForeignStudentId() {
        AddInstitutionReq req = new AddInstitutionReq(1L, OTHER_ID, 2020, 2024);
        when(accountAccessHelper.resolveStudentIdForResumeMutation(OTHER_ID)).thenReturn(OWN_ID);

        InstitutionEnt ent = new InstitutionEnt();
        when(institutionMapper.toEntity(req)).thenReturn(ent);
        when(educationTools.getEducationOrThrow(1L)).thenReturn(new EducationEnt());
        when(studentTools.getStudentOrThrow(OWN_ID)).thenReturn(student(OWN_ID));
        when(institutionRepo.save(ent)).thenReturn(ent);
        InstitutionDTO dto = new InstitutionDTO(1L, OWN_ID, new InstitutionRes(1L, 2020, 2024));
        when(institutionTools.mapToDTO(ent)).thenReturn(dto);
        when(securityHelper.getCurrentUsername()).thenReturn("stu");

        assertThat(institutionService.create(req).studentId()).isEqualTo(OWN_ID);
        verify(accountAccessHelper).requireStudentCanMutateResume(OWN_ID);
        verify(studentTools, never()).getStudentOrThrow(OTHER_ID);
    }

    @Test
    void update_foreignRecordForbidden() {
        when(institutionTools.getInstitutionOrThrow(5L)).thenReturn(institutionOn());
        doThrow(new ForbiddenException("Нет доступа к профилю студента"))
                .when(accountAccessHelper).requireStudentCanMutateResume(OTHER_ID);

        assertThatThrownBy(() -> institutionService.update(5L, new UpdateInstitutionReq(1L, OWN_ID, 2020, 2024)))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void delete_foreignRecordForbidden() {
        when(institutionTools.getInstitutionOrThrow(5L)).thenReturn(institutionOn());
        doThrow(new ForbiddenException("Нет доступа к профилю студента"))
                .when(accountAccessHelper).requireStudentCanMutateResume(OTHER_ID);

        assertThatThrownBy(() -> institutionService.deleteById(5L))
                .isInstanceOf(ForbiddenException.class);
        verify(institutionRepo, never()).delete(any(InstitutionEnt.class));
    }

    @Test
    void getById_hiddenFromCatalog_notFound() {
        when(institutionTools.getInstitutionOrThrow(5L)).thenReturn(institutionOn());
        doThrow(new NotFoundException("Failed to find student by id " + OTHER_ID))
                .when(accountAccessHelper).requireCanReadStudentResumeDetails(OTHER_ID);

        assertThatThrownBy(() -> institutionService.getById(5L))
                .isInstanceOf(NotFoundException.class);
    }

    private static InstitutionEnt institutionOn() {
        InstitutionEnt ent = new InstitutionEnt();
        ent.setStudent(student(InstitutionServiceImplAccessTest.OTHER_ID));
        return ent;
    }

    private static StudentEnt student(UUID id) {
        StudentEnt student = new StudentEnt();
        student.setId(id);
        return student;
    }
}
