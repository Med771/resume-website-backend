package ru.ai.sin.logic.profile;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import ru.ai.sin.exception.models.ProfileIncompleteException;
import ru.ai.sin.logic.portfolio.PortfolioRepo;
import ru.ai.sin.logic.profile.dto.CommunicationReadinessRes;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.skill.SkillEnt;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.tools.UserTools;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProfileCommunicationGateService {

    private static final int MIN_BIO_LENGTH = 30;

    private final UserTools userTools;
    private final StudentRepo studentRepo;
    private final PortfolioRepo portfolioRepo;

    @Transactional(readOnly = true)
    public CommunicationReadinessRes evaluateForCurrentUser() {
        return userTools.findCurrentUserFetchingLinks()
                .map(this::evaluate)
                .orElse(new CommunicationReadinessRes(false, List.of("auth")));
    }

    @Transactional(readOnly = true)
    public void requireReadyForCommunication() {
        CommunicationReadinessRes readiness = evaluateForCurrentUser();
        if (!readiness.ready()) {
            throw new ProfileIncompleteException(readiness.missingFields());
        }
    }

    private CommunicationReadinessRes evaluate(UserEnt user) {
        if (user.getRole() == RoleEnum.ADMIN) {
            return new CommunicationReadinessRes(true, List.of());
        }
        if (user.getRole() == RoleEnum.STUDENT) {
            return evaluateStudent(user.getStudent());
        }
        if (user.getRole() == RoleEnum.RECRUITER) {
            return evaluateRecruiter(user.getRecruiter());
        }
        return new CommunicationReadinessRes(false, List.of("role"));
    }

    private CommunicationReadinessRes evaluateStudent(StudentEnt student) {
        if (student == null) {
            return new CommunicationReadinessRes(false, List.of("resume"));
        }

        List<String> missing = new ArrayList<>();
        var userInfo = student.getUserInformation();
        String firstName = userInfo != null ? userInfo.getFirstName() : null;
        String lastName = userInfo != null ? userInfo.getLastName() : null;
        if (!StringUtils.hasText(firstName) || !StringUtils.hasText(lastName)) {
            missing.add("name");
        }
        if (!StringUtils.hasText(student.getImagePath())) {
            missing.add("photo");
        }
        if (student.getBio() == null || student.getBio().trim().length() < MIN_BIO_LENGTH) {
            missing.add("about");
        }
        if (!StringUtils.hasText(student.getCity())) {
            missing.add("city");
        }
        if (student.getSpeciality() == null) {
            missing.add("speciality");
        }

        List<SkillEnt> skills = studentRepo.findSkillsByStudentId(student.getId());
        if (skills == null || skills.isEmpty()) {
            missing.add("skills");
        }

        boolean hasPortfolio = !portfolioRepo.findAllByStudent_Id(student.getId()).isEmpty();
        boolean hasHhLink = StringUtils.hasText(student.getHhLink());
        if (!hasPortfolio && !hasHhLink) {
            missing.add("portfolio_or_hh");
        }

        return new CommunicationReadinessRes(missing.isEmpty(), List.copyOf(missing));
    }

    private CommunicationReadinessRes evaluateRecruiter(RecruiterEnt recruiter) {
        if (recruiter == null) {
            return new CommunicationReadinessRes(false, List.of("recruiter_profile"));
        }

        List<String> missing = new ArrayList<>();
        if (!StringUtils.hasText(recruiter.getCompanyName())) {
            missing.add("company_name");
        }
        var userInfo = recruiter.getUserInformation();
        if (userInfo == null || !StringUtils.hasText(userInfo.getFirstName())) {
            missing.add("first_name");
        }
        if (userInfo == null || !StringUtils.hasText(userInfo.getLastName())) {
            missing.add("last_name");
        }
        if (userInfo == null || !StringUtils.hasText(userInfo.getEmail())) {
            missing.add("email");
        }
        if (!StringUtils.hasText(recruiter.getCity())) {
            missing.add("city");
        }

        return new CommunicationReadinessRes(missing.isEmpty(), List.copyOf(missing));
    }
}
