package ru.ai.sin.logic.publicapi.vacancy;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.ai.sin.logic.vacancy.dto.FilterVacancyReq;
import ru.ai.sin.logic.vacancy.dto.VacancyCardDTO;
import ru.ai.sin.logic.vitrina.PublicVacancyCatalogService;
import ru.ai.sin.models.PageResponse;

import java.util.UUID;

@RestController
@RequestMapping("/public/vacancies")
@RequiredArgsConstructor
@Tag(name = "PublicVacancies", description = "Публичная витрина вакансий")
public class PublicVacancyController {

    private final PublicVacancyCatalogService publicVacancyCatalogService;

    @Operation(summary = "Список вакансий для витрины")
    @GetMapping
    public ResponseEntity<PageResponse<VacancyCardDTO>> list(
            @PageableDefault(size = 20) Pageable pageable,
            FilterVacancyReq filter
    ) {
        return ResponseEntity.ok(publicVacancyCatalogService.listPublic(pageable, filter));
    }

    @Operation(summary = "Вакансия для витрины по id")
    @GetMapping("/{id}")
    public ResponseEntity<VacancyCardDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(publicVacancyCatalogService.getPublicById(id));
    }
}
