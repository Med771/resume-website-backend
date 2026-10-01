package ru.ai.sin.logic.recruiter;

import ru.ai.sin.logic.recruiter.dto.OnboardingStatusRes;
import ru.ai.sin.logic.vacancy.dto.CreateVacancyReq;
import ru.ai.sin.logic.vacancy.dto.VacancyDTO;

public interface RecruiterVacancyOnboardingService {

    VacancyDTO createFirstVacancy(CreateVacancyReq req);

    boolean hasVacancyForCurrentUser();

    boolean isProfileCompleteForCurrentUser();
}
