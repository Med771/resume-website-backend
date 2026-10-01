package ru.ai.sin.logic.company;

import org.springframework.data.domain.Pageable;

import ru.ai.sin.models.PageResponse;

import ru.ai.sin.logic.company.dto.*;


public interface CompanyService {

    // ---------- GET METHODS ----------
    CompanyDTO getById(long id);

    // ---------- POST METHODS ----------
    PageResponse<CompanyDTO> getAllByFilter(
            Pageable pageable,
            FilterCompanyReq filterCompanyReq);

    CompanyDTO create(AddCompanyReq addCompanyReq);

    CompanyDTO updateById(
            long id,
            UpdateCompanyReq updateCompanyReq);

    // ---------- DELETE METHODS ----------
    void deleteById(long id);
}
