package ru.ai.sin.logic.portfolio;

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

import ru.ai.sin.logic.portfolio.dto.*;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/portfolio")
@Tag(name = "Portfolio", description = "Операции управления портфолио студентов")
public class PortfolioController {

    private final PortfolioService portfolioService;

    @Operation(summary = "Получить портфолио по ID", description = "Своё всегда; чужое — если карточка открыта админом (catalogVisible)")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @GetMapping(path = "/{id}")
    public ResponseEntity<PortfolioDTO> getById(@PathVariable @Min(1) long id) {
        PortfolioDTO portfolioDTO = portfolioService.getById(id);

        return ResponseEntity.ok(portfolioDTO);
    }

    @Operation(summary = "Фильтр портфолио", description = "Принимает DTO фильтра в request body и Pageable без параметра sort")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @PostMapping(path = "/filter")
    public ResponseEntity<PageResponse<PortfolioDTO>> findAllByFilter(
            @PageableDefault Pageable pageable,

            @Valid @RequestBody FilterPortfolioReq filterPortfolioReq
    ) {
        PageResponse<PortfolioDTO> portfolioDTOs = portfolioService.getAllByFilter(pageable, filterPortfolioReq);

        return ResponseEntity.ok(portfolioDTOs);
    }


    @Operation(summary = "Создать портфолио", description = "Студент — только на свою карточку; админ — любой studentId")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @PostMapping()
    public ResponseEntity<PortfolioDTO> create(@Valid @RequestBody AddPortfolioReq portfolioReq) {
        PortfolioDTO portfolioDTO = portfolioService.create(portfolioReq);

        return ResponseEntity.status(HttpStatus.CREATED).body(portfolioDTO);
    }

    @Operation(summary = "Обновить портфолио", description = "Студент — только свою запись")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @PutMapping(path = "/{id}")
    public ResponseEntity<PortfolioDTO> updateById(
            @PathVariable @Min(1) long id,

            @Valid @RequestBody AddPortfolioReq portfolioReq
    ) {
        PortfolioDTO portfolioDTO = portfolioService.update(id, portfolioReq);

        return ResponseEntity.ok(portfolioDTO);
    }

    @Operation(summary = "Удалить портфолио", description = "Студент — только свою запись")
    @PreAuthorize("hasAnyRole('STUDENT', 'ADMIN')")
    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable @Min(1) long id) {
        portfolioService.deleteById(id);
    }
}
