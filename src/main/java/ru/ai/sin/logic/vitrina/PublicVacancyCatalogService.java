package ru.ai.sin.logic.vitrina;

import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.vacancy.VacancyEnt;
import ru.ai.sin.logic.vacancy.VacancyRepo;
import ru.ai.sin.logic.vacancy.VacancyServiceImpl;
import ru.ai.sin.logic.vacancy.VacancySpecifications;
import ru.ai.sin.logic.vacancy.dto.FilterVacancyReq;
import ru.ai.sin.logic.vacancy.dto.VacancyCardDTO;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.models.enums.VacancyStatus;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PublicVacancyCatalogService {

    private final VacancyRepo vacancyRepo;
    private final AccountAccessHelper accountAccessHelper;

    @Transactional(readOnly = true)
    public PageResponse<VacancyCardDTO> listPublic(Pageable pageable, FilterVacancyReq filter) {
        UserEnt user = accountAccessHelper.requireCurrentUserOptional();
        if (user == null || accountAccessHelper.treatsAsAnonymousForCatalog(user)) {
            return listAnonymous(pageable, filter);
        }
        if (user.getRole() == RoleEnum.ADMIN) {
            return listAllPublished(pageable, filter, null);
        }
        if (user.getRole() == RoleEnum.STUDENT && accountAccessHelper.isApprovedOrAdmin(user)) {
            return listAllPublished(pageable, filter, null);
        }
        if (user.getRole() == RoleEnum.RECRUITER && accountAccessHelper.isApprovedOrAdmin(user)) {
            return listRecruiterOpen(pageable, filter);
        }
        return listAnonymous(pageable, filter);
    }

    @Transactional(readOnly = true)
    public VacancyCardDTO getPublicById(UUID id) {
        VacancyEnt v = vacancyRepo.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Вакансия не найдена: " + id));
        UserEnt user = accountAccessHelper.requireCurrentUserOptional();
        if (user == null || accountAccessHelper.treatsAsAnonymousForCatalog(user)) {
            if (!isAnonymousVisible(v)) {
                throw new NotFoundException("Вакансия не найдена: " + id);
            }
        } else if (user.getRole() == RoleEnum.RECRUITER && accountAccessHelper.isApprovedOrAdmin(user)) {
            if (v.getStatus() != VacancyStatus.PUBLISHED || !VacancyServiceImpl.isInPublicationWindow(v)) {
                throw new NotFoundException("Вакансия не найдена: " + id);
            }
        } else if (user.getRole() != RoleEnum.ADMIN) {
            if (v.getStatus() != VacancyStatus.PUBLISHED || !VacancyServiceImpl.isInPublicationWindow(v)) {
                throw new NotFoundException("Вакансия не найдена: " + id);
            }
        }
        return toCard(v);
    }

    private PageResponse<VacancyCardDTO> listAnonymous(Pageable pageable, FilterVacancyReq filter) {
        Page<VacancyEnt> page = vacancyRepo.findAll(
                VacancySpecifications.anonymousVisible(filter),
                pageable
        );
        return mapPage(page, pageable);
    }

    private PageResponse<VacancyCardDTO> listAllPublished(Pageable pageable, FilterVacancyReq filter, UUID studentId) {
        Page<VacancyEnt> page = vacancyRepo.findAll(
                VacancySpecifications.publishedFeed(filter),
                pageable
        );
        return mapPage(page, pageable);
    }

    private PageResponse<VacancyCardDTO> listRecruiterOpen(Pageable pageable, FilterVacancyReq filter) {
        Page<VacancyEnt> page = vacancyRepo.findAll(
                VacancySpecifications.publishedFeed(filter),
                pageable
        );
        return mapPage(page, pageable);
    }

    private static boolean isAnonymousVisible(VacancyEnt v) {
        return v.isVisibleToAnonymous()
                && v.getStatus() == VacancyStatus.PUBLISHED
                && VacancyServiceImpl.isInPublicationWindow(v);
    }

    private PageResponse<VacancyCardDTO> mapPage(Page<VacancyEnt> page, Pageable pageable) {
        return new PageResponse<>(
                page.getContent().stream().map(this::toCard).toList(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private VacancyCardDTO toCard(VacancyEnt v) {
        String summary = v.getDescription();
        if (summary != null && summary.length() > 300) {
            summary = summary.substring(0, 300) + "…";
        }
        return new VacancyCardDTO(
                v.getId(),
                v.getTitle(),
                summary,
                v.getCompanyName(),
                v.getCity(),
                v.getWorkFormat(),
                v.getEmploymentType(),
                v.getSpeciality() != null ? v.getSpeciality().getName() : null,
                0,
                false,
                v.getPublishedFrom()
        );
    }
}
