package ru.ai.sin.helper;

import ru.ai.sin.logic.request.RequestEnt;
import ru.ai.sin.logic.vacancy.VacancyApplicationEnt;
import ru.ai.sin.models.enums.ResultEnum;
import ru.ai.sin.models.enums.TuPhase;
import ru.ai.sin.models.enums.VacancyApplicationStatus;

public final class TuPhaseResolver {

    private TuPhaseResolver() {}

    public static TuPhase fromRequest(RequestEnt r) {
        if (r == null) {
            return TuPhase.NOT_APPLICABLE;
        }
        ResultEnum result = r.getResult();
        if (result == ResultEnum.REFUSAL) {
            return TuPhase.REJECTED;
        }
        if (result == ResultEnum.SUCCESS) {
            return TuPhase.COMPLETED;
        }
        if (result != ResultEnum.STUDENT_CONFIRMED && result != ResultEnum.RECRUITER_CONFIRMED) {
            return TuPhase.NOT_APPLICABLE;
        }
        if (r.getStudentTuConfirmedAt() == null) {
            return TuPhase.WAITING_STUDENT;
        }
        if (r.getRecruiterTuConfirmedAt() == null) {
            return TuPhase.WAITING_RECRUITER;
        }
        return TuPhase.COMPLETED;
    }

    public static TuPhase fromVacancyApplication(VacancyApplicationEnt app) {
        if (app == null) {
            return TuPhase.NOT_APPLICABLE;
        }
        if (app.getStatus() == VacancyApplicationStatus.REJECTED) {
            return TuPhase.REJECTED;
        }
        if (app.getStatus() != VacancyApplicationStatus.ACCEPTED) {
            return TuPhase.NOT_APPLICABLE;
        }
        if (app.getStudentTuConfirmedAt() != null && app.getRecruiterTuConfirmedAt() != null) {
            return TuPhase.COMPLETED;
        }
        if (app.getStudentTuConfirmedAt() == null) {
            return TuPhase.WAITING_STUDENT;
        }
        return TuPhase.WAITING_RECRUITER;
    }
}
