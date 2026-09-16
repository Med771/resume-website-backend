package ru.ai.sin.logic.publicapi.vitrina;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.config.property.VitrinaProperties;
import ru.ai.sin.logic.publicapi.student.PublicCatalogStudentService;
import ru.ai.sin.logic.publicapi.vitrina.dto.PublicHomeVitrinaDTO;
import ru.ai.sin.logic.siteproject.SiteProjectService;
import ru.ai.sin.logic.student.dto.FilterStudentReq;
import ru.ai.sin.logic.student.dto.StudentCardDTO;
import ru.ai.sin.models.PageResponse;

@Service
@RequiredArgsConstructor
public class PublicHomeVitrinaService {

    private final PublicCatalogStudentService publicCatalogStudentService;
    private final SiteProjectService siteProjectService;
    private final VitrinaProperties vitrinaProperties;

    @Transactional(readOnly = true)
    public PublicHomeVitrinaDTO getHome() {
        int studentsLimit = Math.max(1, vitrinaProperties.getStudentsLimit());
        int projectsLimit = Math.max(1, vitrinaProperties.getProjectsLimit());

        PageResponse<StudentCardDTO> studentsPage = publicCatalogStudentService.listCards(
                PageRequest.of(0, studentsLimit),
                emptyFilter());

        var projects = siteProjectService.listForVitrina(projectsLimit);

        return new PublicHomeVitrinaDTO(studentsPage.data(), projects);
    }

    private static FilterStudentReq emptyFilter() {
        return new FilterStudentReq(null, null, null, null, null, null, null, null, null, null, null, null);
    }
}
