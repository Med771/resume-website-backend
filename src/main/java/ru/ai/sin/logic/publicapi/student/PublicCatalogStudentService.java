package ru.ai.sin.logic.publicapi.student;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.student.StudentSortResolver;
import ru.ai.sin.logic.student.StudentSpecifications;
import ru.ai.sin.logic.student.dto.FilterStudentReq;
import ru.ai.sin.logic.student.dto.StudentCardDTO;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.tools.StudentTools;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicCatalogStudentService {

    private final StudentRepo studentRepo;
    private final StudentTools studentTools;

    @Transactional(readOnly = true)
    public StudentCardDTO getCardById(UUID id) {
        StudentEnt student = studentRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Failed to find student by id " + id));
        if (!student.isPublicProfileConsent() || !student.isCatalogVisible()) {
            throw new NotFoundException("Failed to find student by id " + id);
        }
        return studentTools.mapToCardDTO(student);
    }

    @Transactional(readOnly = true)
    public PageResponse<StudentCardDTO> listCards(Pageable pageable, FilterStudentReq filterStudentReq) {
        Sort sort = StudentSortResolver.resolve(filterStudentReq);
        Pageable effective = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
        Page<StudentEnt> page = studentRepo.findAll(
                StudentSpecifications.byFilters(filterStudentReq, false, true),
                effective);
        return new PageResponse<>(
                page.getContent().stream().map(studentTools::mapToCardDTO).toList(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }
}
