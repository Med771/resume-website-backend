package ru.ai.sin.tools;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.ParticipantDisplayNames;
import ru.ai.sin.helper.TuPhaseResolver;
import ru.ai.sin.logic.request.RequestEnt;
import ru.ai.sin.logic.request.RequestRepo;
import ru.ai.sin.logic.request.dto.RequestDTO;

@Component
@RequiredArgsConstructor
public class RequestTools {

    private final RequestRepo requestRepo;

    @Transactional(readOnly = true)
    public RequestEnt getRequestOrThrow(long id) {
        RequestEnt requestEnt = requestRepo.findById(id);
        if (requestEnt == null) {
            throw new NotFoundException("Failed to find request with id " + id);
        }
        return requestEnt;
    }

    public RequestDTO mapToDTO(RequestEnt requestEnt) {
        var ts = requestEnt.getTimestamps();
        return new RequestDTO(
                requestEnt.getId(),
                requestEnt.getAppChat().getId(),
                requestEnt.getResult(),
                ts != null ? ts.getCreatedAt() : null,
                ts != null ? ts.getUpdatedAt() : null,
                requestEnt.getStudentResponseText(),
                requestEnt.getRecruiter().getId(),
                requestEnt.getStudent().getId(),
                requestEnt.getStudentTuConfirmedAt(),
                requestEnt.getRecruiterTuConfirmedAt(),
                requestEnt.getRejectionReasonCode(),
                requestEnt.getRejectionComment(),
                ParticipantDisplayNames.recruiter(requestEnt.getRecruiter()),
                ParticipantDisplayNames.student(requestEnt.getStudent()),
                TuPhaseResolver.fromRequest(requestEnt)
        );
    }
}
