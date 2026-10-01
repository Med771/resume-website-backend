package ru.ai.sin.logic.vacancy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.logic.chat.ChatService;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.vacancy.dto.ApplyVacancyReq;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.models.enums.VacancyStatus;
import ru.ai.sin.tools.StudentTools;
import ru.ai.sin.tools.VacancyApplicationTools;
import ru.ai.sin.tools.UserTools;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VacancyApplicationServiceImplTest {

    @Mock
    private VacancyApplicationRepo vacancyApplicationRepo;
    @Mock
    private VacancyRepo vacancyRepo;
    @Mock
    private UserTools userTools;
    @Mock
    private StudentTools studentTools;
    @Mock
    private ChatService chatService;
    @Mock
    private AccountAccessHelper accountAccessHelper;
    @Mock
    private VacancyApplicationTools vacancyApplicationTools;

    private VacancyApplicationServiceImpl service;

    private final UUID vacancyId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID studentId = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");

    @BeforeEach
    void setUp() {
        service = new VacancyApplicationServiceImpl(
                vacancyApplicationRepo, vacancyRepo, userTools, studentTools,
                vacancyApplicationTools, chatService, accountAccessHelper);
    }

    @Test
    void apply_rejectsHiddenFromCatalogStudent() {
        StudentEnt student = new StudentEnt();
        student.setId(studentId);
        student.setCatalogVisible(false);
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "stu", "p");
        user.setStudent(student);

        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> service.apply(vacancyId, new ApplyVacancyReq(null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("каталог");
    }

    @Test
    void apply_rejectsUnpublishedVacancy() {
        StudentEnt student = new StudentEnt();
        student.setId(studentId);
        student.setCatalogVisible(true);
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "stu", "p");
        user.setStudent(student);

        RecruiterEnt recruiter = new RecruiterEnt();
        recruiter.setId(UUID.randomUUID());
        VacancyEnt vacancy = new VacancyEnt();
        vacancy.setId(vacancyId);
        vacancy.setRecruiter(recruiter);
        vacancy.setStatus(VacancyStatus.DRAFT);

        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(user));
        when(vacancyRepo.findWithDetailsById(vacancyId)).thenReturn(Optional.of(vacancy));

        assertThatThrownBy(() -> service.apply(vacancyId, new ApplyVacancyReq("letter")))
                .isInstanceOf(BadRequestException.class);
    }
}
