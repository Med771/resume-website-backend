package ru.ai.sin.logic.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class AnalyticsEventStore {

    private final AnalyticsEventRepo analyticsEventRepo;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(AnalyticsEventEnt event) {
        analyticsEventRepo.save(event);
    }
}
