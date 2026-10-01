package ru.ai.sin.logic.profile;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.logic.portfolio.PortfolioRepo;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.skill.SkillEnt;
import ru.ai.sin.logic.speciality.SpecialityEnt;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.models.embeddables.UserInformation;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.tools.UserTools;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileCommunicationGateServiceTest {

    @Mock
    private UserTools userTools;
    @Mock
    private StudentRepo studentRepo;
    @Mock
    private PortfolioRepo portfolioRepo;

    @InjectMocks
    private ProfileCommunicationGateService service;

    @Test
    void evaluateStudent_readyWhenAllFieldsPresent() {
        UUID studentId = UUID.randomUUID();
        StudentEnt student = completeStudent(studentId);
        UserEnt user = studentUser(student);

        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(user));
        when(studentRepo.findSkillsByStudentId(studentId)).thenReturn(List.of(new SkillEnt("Java")));
        when(portfolioRepo.findAllByStudent_Id(studentId)).thenReturn(List.of());

        var res = service.evaluateForCurrentUser();

        assertThat(res.ready()).isTrue();
        assertThat(res.missingFields()).isEmpty();
    }

    @Test
    void evaluateRecruiter_readyWithoutVacancy() {
        RecruiterEnt recruiter = completeRecruiter();
        UserEnt user = recruiterUser(recruiter);

        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(user));

        var res = service.evaluateForCurrentUser();

        assertThat(res.ready()).isTrue();
        assertThat(res.missingFields()).isEmpty();
    }

    @Test
    void evaluateRecruiter_missingCompanyName() {
        RecruiterEnt recruiter = completeRecruiter();
        recruiter.setCompanyName(null);
        UserEnt user = recruiterUser(recruiter);

        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(user));

        var res = service.evaluateForCurrentUser();

        assertThat(res.ready()).isFalse();
        assertThat(res.missingFields()).contains("company_name");
    }

    @Test
    void evaluateStudent_missingPhotoAndPortfolio() {
        UUID studentId = UUID.randomUUID();
        StudentEnt student = completeStudent(studentId);
        student.setImagePath(null);
        student.setHhLink(null);
        UserEnt user = studentUser(student);

        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.of(user));
        when(studentRepo.findSkillsByStudentId(studentId)).thenReturn(List.of(new SkillEnt("Java")));
        when(portfolioRepo.findAllByStudent_Id(studentId)).thenReturn(List.of());

        var res = service.evaluateForCurrentUser();

        assertThat(res.ready()).isFalse();
        assertThat(res.missingFields()).contains("photo", "portfolio_or_hh");
    }

    private static UserEnt studentUser(StudentEnt student) {
        UserEnt user = new UserEnt(RoleEnum.STUDENT, "s", "student", "hash");
        user.setStudent(student);
        return user;
    }

    private static UserEnt recruiterUser(RecruiterEnt recruiter) {
        UserEnt user = new UserEnt(RoleEnum.RECRUITER, "r", "recruiter", "hash");
        user.setRecruiter(recruiter);
        return user;
    }

    private static RecruiterEnt completeRecruiter() {
        RecruiterEnt recruiter = new RecruiterEnt();
        recruiter.setId(UUID.randomUUID());
        recruiter.setCompanyName("ООО Тест");
        recruiter.setCity("Москва");
        UserInformation info = new UserInformation();
        info.setFirstName("Пётр");
        info.setLastName("Петров");
        info.setEmail("hr@test.ru");
        recruiter.setUserInformation(info);
        return recruiter;
    }

    private static StudentEnt completeStudent(UUID id) {
        StudentEnt student = new StudentEnt();
        student.setId(id);
        student.setCity("Москва");
        student.setBio("Достаточно длинное описание профиля студента");
        student.setImagePath("photo.jpg");
        student.setHhLink("https://hh.ru/resume/1");
        SpecialityEnt speciality = new SpecialityEnt();
        speciality.setId(1L);
        student.setSpeciality(speciality);
        UserInformation info = new UserInformation();
        info.setFirstName("Иван");
        info.setLastName("Иванов");
        student.setUserInformation(info);
        return student;
    }
}
