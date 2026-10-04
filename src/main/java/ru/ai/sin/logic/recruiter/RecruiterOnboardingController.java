package ru.ai.sin.logic.recruiter;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.ai.sin.logic.recruiter.dto.OnboardingStatusRes;
import ru.ai.sin.logic.vacancy.dto.CreateVacancyReq;
import ru.ai.sin.logic.vacancy.dto.VacancyDTO;

@RestController
@RequestMapping("/recruiter/onboarding")
@RequiredArgsConstructor
@Tag(name = "RecruiterOnboarding", description = "Онбординг первой вакансии после одобрения регистрации")
public class RecruiterOnboardingController {

    private final RecruiterVacancyOnboardingService recruiterVacancyOnboardingService;

    @Operation(summary = "Создать первую вакансию", description = "Только RECRUITER без существующих вакансий")
    @PreAuthorize("hasRole('RECRUITER')")
    @PostMapping("/vacancy")
    @ResponseStatus(HttpStatus.CREATED)
    public ResponseEntity<VacancyDTO> createFirstVacancy(@Valid @RequestBody CreateVacancyReq req) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(recruiterVacancyOnboardingService.createFirstVacancy(req));
    }

    @Operation(summary = "Статус онбординга вакансии")
    @PreAuthorize("hasRole('RECRUITER')")
    @GetMapping("/status")
    public ResponseEntity<OnboardingStatusRes> status() {
        boolean profileCompleted = recruiterVacancyOnboardingService.isProfileCompleteForCurrentUser();
        boolean vacancyCompleted = recruiterVacancyOnboardingService.hasVacancyForCurrentUser();
        return ResponseEntity.ok(new OnboardingStatusRes(
                profileCompleted,
                vacancyCompleted,
                profileCompleted));
    }
}
