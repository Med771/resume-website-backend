package ru.ai.sin.logic.company;

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

import ru.ai.sin.logic.company.dto.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/company")
@Tag(name = "Company", description = "Операции управления компаниями")
public class CompanyController {

    private final CompanyService companyService;

    @Operation(summary = "Получить компанию по ID", description = "Возвращает компанию с данными по связям")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @GetMapping(path = "/{id}")
    public ResponseEntity<CompanyDTO> getById(@PathVariable @Min(1) long id) {
        CompanyDTO companyDTO = companyService.getById(id);

        return ResponseEntity.ok(companyDTO);
    }

    @Operation(summary = "Фильтр компаний", description = "Принимает DTO фильтра в request body и Pageable без параметра sort")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @PostMapping(path = "/filter")
    public ResponseEntity<PageResponse<CompanyDTO>> filter(
            @PageableDefault Pageable pageable,

            @Valid @RequestBody FilterCompanyReq filterCompanyReq
    ) {
        PageResponse<CompanyDTO> companyDTOs = companyService.getAllByFilter(
                pageable, filterCompanyReq);

        return ResponseEntity.ok(companyDTOs);
    }

    @Operation(summary = "Создать компанию", description = "Создает новую компанию")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping()
    public ResponseEntity<CompanyDTO> create(@Valid @RequestBody AddCompanyReq companyReq) {
        CompanyDTO companyDTO = companyService.create(companyReq);

        return ResponseEntity.status(HttpStatus.CREATED).body(companyDTO);
    }

    @Operation(summary = "Обновить компанию", description = "Обновляет данные компании по ID")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{id}")
    public ResponseEntity<CompanyDTO> updateById(
            @PathVariable @Min(1) long id,

            @Valid @RequestBody UpdateCompanyReq updateCompanyReq
    ) {
        CompanyDTO companyDTO = companyService.updateById(id, updateCompanyReq);

        return ResponseEntity.ok(companyDTO);
    }

    @Operation(summary = "Удалить компанию", description = "Удаляет компанию по ID")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable @Min(1) long id) {
        companyService.deleteById(id);
    }
}
