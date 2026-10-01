package ru.ai.sin.logic.recruiter.registration;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.ai.sin.logic.recruiter.registration.dto.FilterRecruiterRegistrationReq;
import ru.ai.sin.logic.recruiter.registration.dto.RecruiterRegistrationApproveResultDTO;
import ru.ai.sin.logic.recruiter.registration.dto.RecruiterRegistrationRejectReq;
import ru.ai.sin.logic.recruiter.registration.dto.RecruiterRegistrationRequestDTO;
import ru.ai.sin.models.PageResponse;

import java.util.UUID;

@RestController
@RequestMapping("/admin/recruiter-registration-requests")
@RequiredArgsConstructor
@Validated
@Tag(name = "RecruiterRegistrationAdmin", description = "Модерация заявок на регистрацию работодателей")
@PreAuthorize("hasRole('ADMIN')")
public class RecruiterRegistrationAdminController {

    private final RecruiterRegistrationAdminService recruiterRegistrationAdminService;

    @Operation(summary = "Список заявок на регистрацию рекрутера")
    @PostMapping("/filter")
    public ResponseEntity<PageResponse<RecruiterRegistrationRequestDTO>> filter(
            @PageableDefault Pageable pageable,
            @Valid @RequestBody(required = false) FilterRecruiterRegistrationReq filter
    ) {
        return ResponseEntity.ok(recruiterRegistrationAdminService.filter(pageable, filter));
    }

    @Operation(summary = "Одобрить заявку", description = "Создаёт Recruiter + User(RECRUITER) с паролем из заявки; заявка переходит в APPROVED")
    @PostMapping("/{id}/approve")
    public ResponseEntity<RecruiterRegistrationApproveResultDTO> approve(@PathVariable UUID id) {
        return ResponseEntity.ok(recruiterRegistrationAdminService.approve(id));
    }

    @Operation(summary = "Отклонить заявку")
    @PostMapping("/{id}/reject")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reject(
            @PathVariable UUID id,
            @RequestBody(required = false) RecruiterRegistrationRejectReq body
    ) {
        recruiterRegistrationAdminService.reject(id, body);
    }
}
