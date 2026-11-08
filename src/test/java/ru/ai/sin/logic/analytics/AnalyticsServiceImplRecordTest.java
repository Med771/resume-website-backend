package ru.ai.sin.logic.analytics;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.logic.analytics.dto.AnalyticsEventInReq;
import ru.ai.sin.logic.recruiter.RecruiterRepo;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.tools.UserTools;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AnalyticsServiceImplRecordTest {

    @Mock
    private AnalyticsEventRepo analyticsEventRepo;
    @Mock
    private AnalyticsMinuteIpRateLimiter analyticsMinuteIpRateLimiter;
    @Mock
    private UserRepo userRepo;
    @Mock
    private StudentRepo studentRepo;
    @Mock
    private RecruiterRepo recruiterRepo;
    @Mock
    private UserTools userTools;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private AnalyticsServiceImpl service;

    @Test
    void pageView_savesWithoutPayload() {
        when(userTools.findCurrentUserFetchingLinks()).thenReturn(Optional.empty());
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getRemoteAddr()).thenReturn("127.0.0.1");

        service.recordEvent(new AnalyticsEventInReq("PAGE_VIEW", "/students", null, null), request);

        ArgumentCaptor<AnalyticsEventEnt> captor = ArgumentCaptor.forClass(AnalyticsEventEnt.class);
        verify(analyticsEventRepo).save(captor.capture());
        assertThat(captor.getValue().getEventType()).isEqualTo("PAGE_VIEW");
        assertThat(captor.getValue().getPath()).isEqualTo("/students");
        assertThat(captor.getValue().getPayload()).isNull();
    }

    @Test
    void emailSentFromClient_isRejected() {
        HttpServletRequest request = mock(HttpServletRequest.class);

        assertThatThrownBy(() -> service.recordEvent(
                new AnalyticsEventInReq("EMAIL_SENT", "/mail", null, null), request))
                .isInstanceOf(BadRequestException.class);
        verify(analyticsEventRepo, never()).save(any());
    }
}
