package ru.ai.sin.helper;

import org.junit.jupiter.api.Test;
import ru.ai.sin.logic.request.RequestEnt;
import ru.ai.sin.models.enums.ResultEnum;
import ru.ai.sin.models.enums.TuPhase;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class TuPhaseResolverTest {

    @Test
    void fromRequest_completedWhenSuccess() {
        RequestEnt r = new RequestEnt();
        r.setResult(ResultEnum.SUCCESS);
        assertThat(TuPhaseResolver.fromRequest(r)).isEqualTo(TuPhase.COMPLETED);
    }

    @Test
    void fromRequest_waitingRecruiterWhenStudentConfirmedTu() {
        RequestEnt r = new RequestEnt();
        r.setResult(ResultEnum.STUDENT_CONFIRMED);
        r.setStudentTuConfirmedAt(LocalDateTime.now());
        assertThat(TuPhaseResolver.fromRequest(r)).isEqualTo(TuPhase.WAITING_RECRUITER);
    }

    @Test
    void fromRequest_notApplicableWhenWaiting() {
        RequestEnt r = new RequestEnt();
        r.setResult(ResultEnum.WAITING);
        assertThat(TuPhaseResolver.fromRequest(r)).isEqualTo(TuPhase.NOT_APPLICABLE);
    }
}
