package ru.ai.sin.logic.skill;

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

import ru.ai.sin.logic.skill.dto.*;


@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping(path = "/skill")
@Tag(name = "Skill", description = "Операции управления навыками")
public class SkillController {

    private final SkillService skillService;

    @Operation(summary = "Получить навык по ID", description = "Возвращает карточку навыка")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @GetMapping(path = "/{id}")
    public ResponseEntity<SkillDTO> getById(@PathVariable @Min(1) long id) {
        SkillDTO skillDTO = skillService.getById(id);

        return ResponseEntity.ok(skillDTO);
    }

    @Operation(summary = "Фильтр навыков", description = "Принимает DTO фильтра в request body и Pageable без параметра sort")
    @PreAuthorize("hasAnyRole('STUDENT', 'RECRUITER', 'ADMIN')")
    @PostMapping(path = "/filter")
    public ResponseEntity<PageResponse<SkillDTO>> filter(
            @PageableDefault Pageable pageable,

            @Valid @RequestBody FilterSkillReq filterSkillReq
    ) {
        PageResponse<SkillDTO> skillDTOs = skillService.getAllByFilter(pageable, filterSkillReq);

        return ResponseEntity.ok(skillDTOs);
    }

    @Operation(summary = "Создать навык", description = "Создает новый навык")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping()
    public ResponseEntity<SkillDTO> create(@Valid @RequestBody AddSkillReq skillReq) {
        SkillDTO skillDTO = skillService.create(skillReq);

        return ResponseEntity.status(HttpStatus.CREATED).body(skillDTO);
    }

    @Operation(summary = "Обновить навык", description = "Обновляет навык по ID")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping(path = "/{id}")
    public ResponseEntity<SkillDTO> updateById(
            @PathVariable @Min(1) long id,

            @Valid @RequestBody UpdateSkillReq updateSkillReq
    ) {
        SkillDTO skillDTO = skillService.updateById(id, updateSkillReq);

        return ResponseEntity.ok(skillDTO);
    }

    @Operation(summary = "Удалить навык", description = "Удаляет навык по ID")
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping(path = "/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteById(@PathVariable long id) {
        skillService.deleteById(id);
    }
}
