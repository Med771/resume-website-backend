package ru.ai.sin.logic.analytics.dto;

import java.util.List;

public record AnalyticsFunnelDTO(
        List<AnalyticsFunnelRow> byEventType
) {
}
