package ru.ai.sin.logic.recruiter;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.vacancy.VacancyRepo;
import ru.ai.sin.logic.vacancy.VacancyService;
import ru.ai.sin.logic.vacancy.dto.CreateVacancyReq;
import ru.ai.sin.logic.vacancy.dto.VacancyDTO;
import ru.ai.sin.models.enums.RoleEnum;
import org.springframework.util.StringUtils;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.tools.UserTools;

@Service
@RequiredArgsConstructor
public class RecruiterVacancyOnboardingServiceImpl implements RecruiterVacancyOnboardingService {

    private final VacancyService vacancyService;
    private final VacancyRepo vacancyRepo;
    private final UserTools userTools;

    @Override
    @Transactional
    public VacancyDTO createFirstVacancy(CreateVacancyReq req) {
        RecruiterEnt recruiter = requireCurrentRecruiter();
        if (vacancyRepo.countByRecruiter_Id(recruiter.getId()) > 0) {
            throw new BadRequestException("Первая вакансия уже создана");
        }
        return vacancyService.create(req);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasVacancyForCurrentUser() {
        return userTools.findCurrentUserFetchingRecruiter()
                .filter(u -> u.getRole() == RoleEnum.RECRUITER && u.getRecruiter() != null)
                .map(u -> vacancyRepo.countByRecruiter_Id(u.getRecruiter().getId()) > 0)
                .orElse(false);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isProfileCompleteForCurrentUser() {
        return userTools.findCurrentUserFetchingRecruiter()
                .filter(u -> u.getRole() == RoleEnum.RECRUITER && u.getRecruiter() != null)
                .map(u -> isProfileComplete(u.getRecruiter(), u))
                .orElse(false);
    }

    static boolean isProfileComplete(RecruiterEnt recruiter, UserEnt user) {
        if (recruiter == null) {
            return false;
        }
        String companyName = recruiter.getCompanyName();
        if (!StringUtils.hasText(companyName)) {
            return false;
        }
        if (user != null && companyName.equalsIgnoreCase(user.getUsername())) {
            return false;
        }
        var userInfo = recruiter.getUserInformation();
        if (userInfo == null
                || !StringUtils.hasText(userInfo.getFirstName())
                || !StringUtils.hasText(userInfo.getLastName())
                || !StringUtils.hasText(userInfo.getEmail())) {
            return false;
        }
        return StringUtils.hasText(recruiter.getCity());
    }

    private RecruiterEnt requireCurrentRecruiter() {
        UserEnt user = userTools.findCurrentUserFetchingRecruiter()
                .orElseThrow(() -> new BadRequestException("Требуется вход в систему"));
        if (user.getRole() != RoleEnum.RECRUITER || user.getRecruiter() == null) {
            throw new BadRequestException("Онбординг вакансии доступен только рекрутёрам");
        }
        return user.getRecruiter();
    }
}
