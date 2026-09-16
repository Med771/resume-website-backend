package ru.ai.sin.logic.education;

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

import ru.ai.sin.logic.education.dto.*;


@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/education")
@Tag(name = "Education", description = "Операции управления справочником образования")
public class EducationController {

    private final EducationService educationService;

    @Operation(summary = "Получить образование по ID", description = "Возвращает запись справочника образования")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @GetMapping(path = "/{id}")
    public ResponseEntity<EducationDTO> getById(@PathVariable @Min(1) long id) {
        EducationDTO educationDTO = educationService.getById(id);

        return ResponseEntity.ok(educationDTO);
    }

    @Operation(summary = "Фильтр образования", description = "Принимает DTO фильтра в request body и Pageable без параметра sort")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @PostMapping(path = "/filter")
    public ResponseEntity<PageResponse<EducationDTO>> filter(
            @PageableDefault Pageable pageable,

            @Valid @RequestBody FilterEducationReq filterEducationReq
    ) {
        PageResponse<EducationDTO> educationDTOs = educationService.getAllByFilter(pageable, filterEducationReq);

        return ResponseEntity.ok(educationDTOs);
    }

    @Operation(summary = "Создать образование", description = "Создает новую запись справочника образования")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping()
    public ResponseEntity<EducationDTO> create(@Valid @RequestBody AddEducationReq addEducationReq) {
        EducationDTO educationDTO = educationService.create(addEducationReq);

        return ResponseEntity.status(HttpStatus.CREATED).body(educationDTO);
    }

    @Operation(summary = "Обновить образование", description = "Обновляет запись образования по ID")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{id}")
    public ResponseEntity<EducationDTO> updateById(
            @PathVariable @Min(1) long id,

            @Valid @RequestBody UpdateEducationReq updateEducationReq
    ) {
        EducationDTO educationDTO = educationService.update(id, updateEducationReq);

        return ResponseEntity.ok(educationDTO);
    }

    @Operation(summary = "Удалить образование", description = "Удаляет запись образования по ID")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable @Min(1) long id) {
        educationService.deleteById(id);
    }
}
