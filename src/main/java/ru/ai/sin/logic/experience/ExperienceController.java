package ru.ai.sin.logic.experience;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;

import lombok.RequiredArgsConstructor;

import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import org.springframework.security.access.prepost.PreAuthorize;

import org.springframework.validation.annotation.Validated;

import org.springframework.web.bind.annotation.*;

import ru.ai.sin.models.PageResponse;

import ru.ai.sin.helper.SecurityHelper;

import ru.ai.sin.logic.experience.dto.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/experience")
@Tag(name = "Experience", description = "Операции управления опытом работы студентов")
public class ExperienceController {

    private final ExperienceService experienceService;

    private final SecurityHelper securityHelper;

    @Operation(summary = "Получить опыт по ID", description = "Своё всегда; чужое — если карточка открыта админом (catalogVisible)")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @GetMapping(path = "/{id}")
    public ResponseEntity<ExperienceDTO> getById(@PathVariable @Min(1) long id) {
        return ResponseEntity.ok(experienceService.getById(id));
    }

    @Operation(summary = "Фильтр опыта", description = "Принимает DTO фильтра в request body и Pageable без параметра sort")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @PostMapping(path = "/filter")
    public ResponseEntity<PageResponse<ExperienceDTO>> findAllByFilter(
            @PageableDefault Pageable pageable,

            @Valid @RequestBody FilterExperienceReq filterExperienceReq) {
        if (filterExperienceReq.companyId() != null) {
            securityHelper.checkAdminRoleForFilter();
        }

        PageResponse<ExperienceDTO> experienceDTOs = experienceService.getAllByFilter(pageable, filterExperienceReq);

        return ResponseEntity.ok(experienceDTOs);
    }

    @Operation(summary = "Создать опыт", description = "Студент — только на свою карточку; админ — любой studentId")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @PostMapping()
    public ResponseEntity<ExperienceDTO> create(@Valid @RequestBody AddExperienceReq addExperienceReq) {
        ExperienceDTO experienceDTO = experienceService.create(addExperienceReq);

        return ResponseEntity.status(HttpStatus.CREATED).body(experienceDTO);
    }

    @Operation(summary = "Обновить опыт", description = "Студент — только свою запись")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @PutMapping(path = "/{id}")
    public ResponseEntity<ExperienceDTO> update(
            @PathVariable long id,

            @Valid @RequestBody UpdateExperienceReq updateExperienceReq
    ) {
        ExperienceDTO experienceDTO = experienceService.update(id, updateExperienceReq);

        return ResponseEntity.ok(experienceDTO);
    }

    @Operation(summary = "Удалить опыт", description = "Студент — только свою запись")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable @Min(1) long id) {
        experienceService.deleteById(id);
    }
}
