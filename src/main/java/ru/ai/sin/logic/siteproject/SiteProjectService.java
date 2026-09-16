package ru.ai.sin.logic.siteproject;

import ru.ai.sin.logic.siteproject.dto.CreateSiteProjectReq;
import ru.ai.sin.logic.siteproject.dto.FilterSiteProjectReq;
import ru.ai.sin.logic.siteproject.dto.ReorderSiteProjectsReq;
import ru.ai.sin.logic.siteproject.dto.SiteProjectDTO;
import ru.ai.sin.logic.siteproject.dto.SiteProjectStudentsReq;
import ru.ai.sin.logic.siteproject.dto.UpdateSiteProjectReq;

import java.util.List;
import java.util.UUID;

public interface SiteProjectService {

    List<SiteProjectDTO> filter(FilterSiteProjectReq req);

    SiteProjectDTO getById(UUID id);

    /** Публичная главная: visibleToAnonymous + окно публикации, без участников, обрезка limit. */
    List<SiteProjectDTO> listForVitrina(int limit);

    SiteProjectDTO create(CreateSiteProjectReq req);

    SiteProjectDTO update(UUID id, UpdateSiteProjectReq req);

    void delete(UUID id);

    void reorder(ReorderSiteProjectsReq req);

    List<UUID> listStudentIds(UUID projectId);

    void bindStudents(UUID projectId, SiteProjectStudentsReq req);

    void unbindStudents(UUID projectId, SiteProjectStudentsReq req);
}
