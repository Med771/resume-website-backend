package ru.ai.sin.logic.city;

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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.ai.sin.logic.city.dto.AddCityReq;
import ru.ai.sin.logic.city.dto.CityDTO;
import ru.ai.sin.logic.city.dto.FilterCityReq;
import ru.ai.sin.logic.city.dto.UpdateCityReq;
import ru.ai.sin.models.PageResponse;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/city")
@Tag(name = "City", description = "Справочник городов студента")
public class CityController {

    private final CityService cityService;

    @Operation(summary = "Получить город по ID", description = "Список открыт без входа: он нужен форме регистрации")
    @GetMapping(path = "/{id}")
    public ResponseEntity<CityDTO> getById(@PathVariable @Min(1) long id) {
        return ResponseEntity.ok(cityService.getById(id));
    }

    @Operation(summary = "Фильтр городов", description = "Необязательная подстрока имени. Без входа")
    @PostMapping(path = "/filter")
    public ResponseEntity<PageResponse<CityDTO>> filter(
            @PageableDefault Pageable pageable,
            @Valid @RequestBody(required = false) FilterCityReq filter
    ) {
        return ResponseEntity.ok(cityService.getAllByFilter(pageable, filter));
    }

    @Operation(summary = "Создать город", description = "Только администратор")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<CityDTO> create(@Valid @RequestBody AddCityReq addCityReq) {
        return ResponseEntity.status(HttpStatus.CREATED).body(cityService.create(addCityReq));
    }

    @Operation(summary = "Обновить город", description = "Только администратор. Имя в анкетах студентов становится таким же")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{id}")
    public ResponseEntity<CityDTO> updateById(
            @PathVariable @Min(1) long id,
            @Valid @RequestBody UpdateCityReq updateCityReq
    ) {
        return ResponseEntity.ok(cityService.update(id, updateCityReq));
    }

    @Operation(summary = "Удалить город", description = "Только администратор. Студенты остаются, ссылка на город становится пустой")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable @Min(1) long id) {
        cityService.deleteById(id);
    }
}
