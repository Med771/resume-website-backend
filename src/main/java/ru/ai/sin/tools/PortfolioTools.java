package ru.ai.sin.tools;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.ai.sin.exception.models.NotFoundException;

import ru.ai.sin.logic.portfolio.PortfolioEnt;
import ru.ai.sin.logic.portfolio.PortfolioRepo;


@Component
@RequiredArgsConstructor
public class PortfolioTools {

    private final PortfolioRepo portfolioRepo;

    @Transactional(readOnly = true)
    public PortfolioEnt getPortfolioOrThrow(long portfolioId) {
        return portfolioRepo.findById(portfolioId).orElseThrow(
                () -> new NotFoundException("Failed to find portfolio by id " + portfolioId)
        );
    }
}
