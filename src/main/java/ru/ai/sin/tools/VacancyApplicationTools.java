package ru.ai.sin.tools;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.ai.sin.helper.TuPhaseResolver;
import ru.ai.sin.logic.vacancy.VacancyApplicationEnt;
import ru.ai.sin.logic.vacancy.dto.VacancyApplicationDTO;

@Component
@RequiredArgsConstructor
public class VacancyApplicationTools {

    private final StudentTools studentTools;

    public VacancyApplicationDTO mapToDTO(VacancyApplicationEnt app) {
        var studentCard = studentTools.mapToCardDTO(app.getStudent());
        var ts = app.getTimestamps();
        return new VacancyApplicationDTO(
                app.getId(),
                app.getVacancy().getId(),
                app.getVacancy().getTitle(),
                app.getStudent().getId(),
                studentCard,
                app.getStatus(),
                app.getCoverLetter(),
                app.getRejectionReason(),
                app.getAppChat() != null ? app.getAppChat().getId() : null,
                ts != null ? ts.getCreatedAt() : null,
                app.getStudentTuConfirmedAt(),
                app.getRecruiterTuConfirmedAt(),
                app.getRejectionReasonCode(),
                app.getRejectionComment(),
                TuPhaseResolver.fromVacancyApplication(app)
        );
    }
}
