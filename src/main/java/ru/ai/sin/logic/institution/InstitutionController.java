package ru.ai.sin.logic.institution;

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

import ru.ai.sin.logic.institution.dto.*;

import ru.ai.sin.helper.SecurityHelper;


@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/institution")
@Tag(name = "Institution", description = "Операции управления обучением студентов")
public class InstitutionController {

    private final InstitutionService institutionService;

    private final SecurityHelper securityHelper;

    @Operation(summary = "Получить запись обучения по ID", description = "Своё всегда; чужое — если карточка открыта админом (catalogVisible)")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @GetMapping(path = "/{id}")
    public ResponseEntity<InstitutionDTO> getById(@PathVariable @Min(1) long id) {
        InstitutionDTO institutionDTO = institutionService.getById(id);

        return ResponseEntity.ok(institutionDTO);
    }

    @Operation(summary = "Фильтр записей обучения", description = "Принимает DTO фильтра в request body и Pageable без параметра sort")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @PostMapping(path = "/filter")
    public ResponseEntity<PageResponse<InstitutionDTO>> findAllByFilter(
            @PageableDefault Pageable pageable,

            @Valid @RequestBody FilterInstitutionReq filterInstitutionReq
    ) {
        if (filterInstitutionReq.educationId() != null) {
            securityHelper.checkAdminRoleForFilter();
        }

        PageResponse<InstitutionDTO> experienceDTOs = institutionService.getAllByFilter(pageable, filterInstitutionReq);

        return ResponseEntity.ok(experienceDTOs);
    }

    @Operation(summary = "Создать запись обучения", description = "Студент — только на свою карточку; админ — любой studentId")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @PostMapping()
    public ResponseEntity<InstitutionDTO> create(@Valid @RequestBody AddInstitutionReq institutionReq) {
        InstitutionDTO institutionDTO = institutionService.create(institutionReq);

        return ResponseEntity.status(HttpStatus.CREATED).body(institutionDTO);
    }

    @Operation(summary = "Обновить запись обучения", description = "Студент — только свою запись")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @PutMapping(path = "/{id}")
    public ResponseEntity<InstitutionDTO> update(
            @PathVariable @Min(1) long id,

            @Valid @RequestBody UpdateInstitutionReq updateInstitutionReq
    ) {
        InstitutionDTO institutionDTO = institutionService.update(id, updateInstitutionReq);

        return ResponseEntity.ok(institutionDTO);
    }

    @Operation(summary = "Удалить запись обучения", description = "Студент — только свою запись")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable @Min(1) long id) {
        institutionService.deleteById(id);
    }
}
