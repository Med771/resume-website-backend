package ru.ai.sin.logic.recruiter.registration;

import jakarta.servlet.http.HttpServletRequest;
import ru.ai.sin.logic.auth.dto.TokenPair;
import ru.ai.sin.logic.recruiter.registration.dto.RecruiterSelfRegistrationReq;

public interface RecruiterSelfRegistrationService {

    TokenPair registerAndIssueTokens(RecruiterSelfRegistrationReq req, HttpServletRequest httpRequest);
}
