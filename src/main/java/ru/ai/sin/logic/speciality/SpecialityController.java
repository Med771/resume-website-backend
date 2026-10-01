package ru.ai.sin.logic.speciality;

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

import ru.ai.sin.logic.speciality.dto.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/speciality")
@Tag(name = "Speciality", description = "Операции управления специальностями")
public class SpecialityController {

    private final SpecialityService specialityService;

    @Operation(summary = "Получить специальность по ID", description = "Возвращает запись специальности")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @GetMapping(path = "/{id}")
    public ResponseEntity<SpecialityDTO> getById(@PathVariable @Min(1) long id) {
        SpecialityDTO specialityDTO = specialityService.getById(id);

        return ResponseEntity.ok(specialityDTO);
    }

    @Operation(summary = "Фильтр специальностей", description = "Принимает DTO фильтра в request body и Pageable без параметра sort")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @PostMapping(path = "/filter")
    public ResponseEntity<PageResponse<SpecialityDTO>> filter(
            @PageableDefault Pageable pageable,

            @Valid @RequestBody FilterSpecialityReq filterSpecialityReq
    ) {
        PageResponse<SpecialityDTO> specialityDTOs = specialityService.getAllByFilter(pageable, filterSpecialityReq);

        return ResponseEntity.ok(specialityDTOs);
    }

    @Operation(summary = "Создать специальность", description = "Создает новую специальность")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping()
    public ResponseEntity<SpecialityDTO> create(@Valid @RequestBody AddSpecialityReq specialityReq) {
        SpecialityDTO specialityDTO = specialityService.create(specialityReq);

        return ResponseEntity.status(HttpStatus.CREATED).body(specialityDTO);
    }

    @Operation(summary = "Обновить специальность", description = "Обновляет специальность по ID")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{id}")
    public ResponseEntity<SpecialityDTO> updateById(
            @PathVariable @Min(1) long id,

            @Valid @RequestBody UpdateSpecialityReq updateSpecialityReq
    ) {
        SpecialityDTO specialityDTO = specialityService.update(id, updateSpecialityReq);

        return ResponseEntity.ok(specialityDTO);
    }

    @Operation(summary = "Удалить специальность", description = "Удаляет специальность по ID")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable @Min(1) long id) {
        specialityService.deleteById(id);
    }
}
