package ru.ai.sin.logic.student.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import ru.ai.sin.logic.experience.dto.ExperienceDTO;
import ru.ai.sin.logic.institution.dto.InstitutionDTO;
import ru.ai.sin.logic.portfolio.dto.PortfolioDTO;

import java.util.List;

@Schema(name = "PublishedStudentSnapshotDTO", description = "Последняя одобренная анкета: карточка, опыт, учёба и портфолио")
public record PublishedStudentSnapshotDTO(
        StudentDTO card,
        List<ExperienceDTO> experience,
        List<InstitutionDTO> education,
        List<PortfolioDTO> portfolio
) {
}
