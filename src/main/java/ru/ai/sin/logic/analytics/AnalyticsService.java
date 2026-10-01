package ru.ai.sin.logic.analytics;

import jakarta.servlet.http.HttpServletRequest;
import ru.ai.sin.logic.analytics.dto.AnalyticsEventInReq;
import ru.ai.sin.logic.analytics.dto.AnalyticsFunnelDTO;
import ru.ai.sin.logic.analytics.dto.AnalyticsSummaryDTO;
import ru.ai.sin.logic.analytics.dto.AnalyticsSummaryReq;
import ru.ai.sin.logic.analytics.dto.EntityPopulationSummaryDTO;
import ru.ai.sin.logic.analytics.dto.EntityPopulationSummaryReq;

public interface AnalyticsService {

    void recordEvent(AnalyticsEventInReq req, HttpServletRequest httpRequest);

    AnalyticsSummaryDTO summarize(AnalyticsSummaryReq req);

    EntityPopulationSummaryDTO summarizeEntityPopulation(EntityPopulationSummaryReq req);

    AnalyticsFunnelDTO summarizeFunnel(AnalyticsSummaryReq req);
}
