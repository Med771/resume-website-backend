package ru.ai.sin.logic.portfolio;

import org.springframework.data.domain.Pageable;

import ru.ai.sin.models.PageResponse;

import ru.ai.sin.logic.portfolio.dto.*;


public interface PortfolioService {

    // ---------- GET METHODS ----------
    PortfolioDTO getById(long id);

    // ---------- POST METHODS ----------
    PageResponse<PortfolioDTO> getAllByFilter(
            Pageable pageable,
            FilterPortfolioReq filterPortfolioReq);

    PortfolioDTO create(AddPortfolioReq  addPortfolioReq);
    PortfolioDTO update(
            long id,
            AddPortfolioReq  addPortfolioReq);

    // ---------- DELETE METHODS ----------
    void deleteById(long id);
}
