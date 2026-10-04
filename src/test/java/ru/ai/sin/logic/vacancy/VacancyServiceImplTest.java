package ru.ai.sin.logic.vacancy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.skill.SkillMapper;
import ru.ai.sin.logic.skill.SkillRepo;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.vacancy.dto.CreateVacancyReq;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.models.enums.VacancyStatus;
import ru.ai.sin.tools.SpecialityTools;
import ru.ai.sin.tools.UserTools;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VacancyServiceImplTest {

    @Mock
    private VacancyRepo vacancyRepo;
    @Mock
    private SkillRepo skillRepo;
    @Mock
    private SkillMapper skillMapper;
    @Mock
    private UserTools userTools;
    @Mock
    private SecurityHelper securityHelper;
    @Mock
    private SpecialityTools specialityTools;

    private VacancyServiceImpl service;

    private final UUID recruiterId = UUID.fromString("11111111-1111-1111-1111-111111111111");

    @BeforeEach
    void setUp() {
        service = new VacancyServiceImpl(
                vacancyRepo, skillRepo, skillMapper, userTools, securityHelper, specialityTools);
    }

    @Test
    void isInPublicationWindow_respectsBounds() {
        VacancyEnt v = new VacancyEnt();
        v.setPublishedFrom(null);
        v.setPublishedTo(null);
        assertThat(VacancyServiceImpl.isInPublicationWindow(v)).isTrue();

        v.setPublishedTo(java.time.LocalDateTime.now().minusDays(1));
        assertThat(VacancyServiceImpl.isInPublicationWindow(v)).isFalse();
    }

    @Test
    void submitForReview_requiresMinDescription() {
        RecruiterEnt recruiter = new RecruiterEnt();
        recruiter.setId(recruiterId);
        recruiter.setCompanyName("Co");
        UserEnt user = new UserEnt(RoleEnum.RECRUITER, "n", "u", "p");
        user.setRecruiter(recruiter);

        VacancyEnt v = new VacancyEnt();
        v.setId(UUID.randomUUID());
        v.setRecruiter(recruiter);
        v.setTitle("Title");
        v.setDescription("short");
        v.setStatus(VacancyStatus.DRAFT);

        when(userTools.findCurrentUserFetchingRecruiter()).thenReturn(Optional.of(user));
        when(vacancyRepo.findWithDetailsById(v.getId())).thenReturn(Optional.of(v));

        assertThatThrownBy(() -> service.submitForReview(v.getId()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("20");
    }

    @Test
    void create_withoutRecruiterProfile_throwsBadRequest() {
        UserEnt user = new UserEnt(RoleEnum.RECRUITER, "n", "guest", "p");
        when(userTools.findCurrentUserFetchingRecruiter()).thenReturn(Optional.of(user));

        CreateVacancyReq req = new CreateVacancyReq(
                "Title", "Description long enough here", null, null, null, null, null, null, null, null, null);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(BadRequestException.class);
    }
}
