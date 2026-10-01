package ru.ai.sin.logic.recruiter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.logic.chat.ChatRepo;
import ru.ai.sin.logic.request.RequestRepo;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.logic.vacancy.VacancyApplicationRepo;
import ru.ai.sin.logic.vacancy.VacancyRepo;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecruiterDeletionService {

    private final VacancyApplicationRepo vacancyApplicationRepo;
    private final VacancyRepo vacancyRepo;
    private final RequestRepo requestRepo;
    private final ChatRepo chatRepo;
    private final UserRepo userRepo;
    private final RecruiterRepo recruiterRepo;

    @Transactional
    public void deleteRecruiterCascade(UUID recruiterId) {
        vacancyApplicationRepo.deleteByVacancyRecruiterId(recruiterId);
        requestRepo.deleteByRecruiter_Id(recruiterId);
        chatRepo.deleteByRecruiter_Id(recruiterId);
        vacancyRepo.deleteByRecruiter_Id(recruiterId);
        userRepo.findByRecruiter_Id(recruiterId).ifPresent(userRepo::delete);
        recruiterRepo.deleteById(recruiterId);
        log.info("Recruiter cascade deleted id={}", recruiterId);
    }
}
