package ru.ai.sin.logic.company;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import ru.ai.sin.models.PageResponse;
import ru.ai.sin.logic.company.dto.AddCompanyReq;
import ru.ai.sin.logic.company.dto.CompanyDTO;
import ru.ai.sin.logic.company.dto.FilterCompanyReq;
import ru.ai.sin.logic.company.dto.UpdateCompanyReq;

import ru.ai.sin.exception.models.BadRequestException;

import ru.ai.sin.helper.SecurityHelper;

import ru.ai.sin.tools.CompanyTools;


@Slf4j
@Service
@RequiredArgsConstructor
public class CompanyServiceImpl implements CompanyService {

    private final CompanyRepo companyRepo;

    private final CompanyTools companyTools;

    private final SecurityHelper securityHelper;

    @Override
    @Transactional(readOnly = true)
    public CompanyDTO getById(long id) {
        return companyTools.mapToDTO(companyTools.getCompanyOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CompanyDTO> getAllByFilter(
            Pageable pageable,
            FilterCompanyReq filterCompanyReq
    ) {
        Page<CompanyEnt> page = companyRepo.findAll(
                CompanySpecifications.byFilters(filterCompanyReq),
                pageable
        );

        return new PageResponse<>(
                companyTools.mapToDTOs(page.getContent()),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Override
    public CompanyDTO create(AddCompanyReq addCompanyReq) {
        CompanyEnt companyEnt = new CompanyEnt(addCompanyReq.name());

        try {
            companyEnt = companyRepo.save(companyEnt);
        }
        catch (DataIntegrityViolationException ex) {
            log.warn("Company already exists: {}", addCompanyReq.name());

            throw new BadRequestException("Company already exists: " + addCompanyReq.name());
        }

        CompanyDTO companyDTO = companyTools.newObjMapToDTO(companyEnt);

        log.info("User: {}, created a new company: {}", securityHelper.getCurrentUsername(), companyDTO);

        return companyDTO;
    }

    @Override
    @Transactional
    public CompanyDTO updateById(
            long id,
            UpdateCompanyReq updateCompanyReq
    ) {
        CompanyEnt companyEnt = companyTools.getCompanyOrThrow(id);

        companyEnt.setName(updateCompanyReq.name());

        CompanyDTO companyDTO = companyTools.mapToDTO(companyEnt);

        log.info("User: {}, updated a company: {} with data: {}", securityHelper.getCurrentUsername(), id, companyDTO);

        return companyDTO;
    }

    @Override
    @Transactional
    public void deleteById(long id) {
        CompanyEnt companyEnt = companyTools.getCompanyOrThrow(id);

        try {
            companyRepo.delete(companyEnt);
        }
        catch (DataIntegrityViolationException ex) {
            log.warn("Error while deleting company: {}", ex.getMessage());

            throw new BadRequestException("Error while deleting company");
        }

        log.info("User: {}, deleted a company: {} with data: {}", securityHelper.getCurrentUsername(), id, companyEnt);
    }
}
