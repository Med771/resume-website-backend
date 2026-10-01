package ru.ai.sin.logic.student;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import ru.ai.sin.logic.student.dto.BulkStudentVisibilityReq;
import ru.ai.sin.logic.student.dto.BulkStudentVisibilityResult;
import ru.ai.sin.logic.student.dto.ReorderStudentsReq;

@RestController
@RequestMapping("/admin/students")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "StudentAdmin", description = "Управление витриной резюме")
public class StudentAdminController {

    private final StudentService studentService;

    @Operation(summary = "Изменить ручной порядок карточек студентов")
    @PostMapping("/reorder")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reorder(@Valid @RequestBody ReorderStudentsReq req) {
        studentService.reorder(req);
    }

    @Operation(summary = "Массово изменить видимость карточек студентов в каталоге и/или на главной")
    @PostMapping("/bulk-visibility")
    public BulkStudentVisibilityResult bulkVisibility(@Valid @RequestBody BulkStudentVisibilityReq req) {
        return studentService.bulkUpdateVisibility(req);
    }
}
