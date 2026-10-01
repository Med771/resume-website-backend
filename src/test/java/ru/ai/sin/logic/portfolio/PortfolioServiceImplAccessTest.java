package ru.ai.sin.logic.portfolio;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.exception.models.ForbiddenException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.portfolio.dto.AddPortfolioReq;
import ru.ai.sin.logic.portfolio.dto.PortfolioDTO;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.tools.PortfolioTools;
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
class PortfolioServiceImplAccessTest {

    private static final UUID OWN_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private PortfolioRepo portfolioRepo;
    @Mock
    private PortfolioMapper portfolioMapper;
    @Mock
    private PortfolioTools portfolioTools;
    @Mock
    private StudentTools studentTools;
    @Mock
    private SecurityHelper securityHelper;
    @Mock
    private AccountAccessHelper accountAccessHelper;

    @InjectMocks
    private PortfolioServiceImpl portfolioService;

    @Test
    void create_studentIgnoresForeignStudentId() {
        AddPortfolioReq req = new AddPortfolioReq("Site", "https://ex.com", null, OTHER_ID);
        when(accountAccessHelper.resolveStudentIdForResumeMutation(OTHER_ID)).thenReturn(OWN_ID);

        StudentEnt own = student(OWN_ID);
        when(studentTools.getStudentOrThrow(OWN_ID)).thenReturn(own);
        PortfolioEnt ent = new PortfolioEnt();
        when(portfolioMapper.toEntity(req, own)).thenReturn(ent);
        when(portfolioRepo.save(ent)).thenReturn(ent);
        PortfolioDTO dto = new PortfolioDTO(1L, "Site", "https://ex.com", null, OWN_ID);
        when(portfolioMapper.toDTO(ent)).thenReturn(dto);
        when(securityHelper.getCurrentUsername()).thenReturn("stu");

        assertThat(portfolioService.create(req).studentId()).isEqualTo(OWN_ID);
        verify(accountAccessHelper).requireStudentCanMutateResume(OWN_ID);
        verify(studentTools, never()).getStudentOrThrow(OTHER_ID);
    }

    @Test
    void update_foreignRecordForbidden() {
        when(portfolioTools.getPortfolioOrThrow(5L)).thenReturn(portfolioOn());
        doThrow(new ForbiddenException("Нет доступа к профилю студента"))
                .when(accountAccessHelper).requireStudentCanMutateResume(OTHER_ID);

        AddPortfolioReq req = new AddPortfolioReq("Site", "https://ex.com", null, OWN_ID);
        assertThatThrownBy(() -> portfolioService.update(5L, req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void delete_foreignRecordForbidden() {
        when(portfolioTools.getPortfolioOrThrow(5L)).thenReturn(portfolioOn());
        doThrow(new ForbiddenException("Нет доступа к профилю студента"))
                .when(accountAccessHelper).requireStudentCanMutateResume(OTHER_ID);

        assertThatThrownBy(() -> portfolioService.deleteById(5L))
                .isInstanceOf(ForbiddenException.class);
        verify(portfolioRepo, never()).delete(any(PortfolioEnt.class));
    }

    @Test
    void getById_hiddenFromCatalog_notFound() {
        when(portfolioTools.getPortfolioOrThrow(5L)).thenReturn(portfolioOn());
        doThrow(new NotFoundException("Failed to find student by id " + OTHER_ID))
                .when(accountAccessHelper).requireCanReadStudentResumeDetails(OTHER_ID);

        assertThatThrownBy(() -> portfolioService.getById(5L))
                .isInstanceOf(NotFoundException.class);
    }

    private static PortfolioEnt portfolioOn() {
        PortfolioEnt ent = new PortfolioEnt();
        ent.setStudent(student(PortfolioServiceImplAccessTest.OTHER_ID));
        return ent;
    }

    private static StudentEnt student(UUID id) {
        StudentEnt student = new StudentEnt();
        student.setId(id);
        return student;
    }
}
