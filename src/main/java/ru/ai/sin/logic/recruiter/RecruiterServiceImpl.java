package ru.ai.sin.logic.recruiter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import ru.ai.sin.models.PageResponse;

import ru.ai.sin.exception.models.BadRequestException;

import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.SecurityHelper;

import ru.ai.sin.logic.recruiter.dto.*;

import ru.ai.sin.tools.RecruiterTools;
import ru.ai.sin.tools.UserTools;

import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecruiterServiceImpl implements RecruiterService {

    private final RecruiterRepo recruiterRepo;

    private final RecruiterMapper recruiterMapper;

    private final RecruiterTools recruiterTools;

    private final UserTools userTools;

    private final SecurityHelper securityHelper;
    private final AccountAccessHelper accountAccessHelper;

    private final RecruiterDeletionService recruiterDeletionService;

    @Override
    @Transactional(readOnly = true)
    public RecruiterDTO getById(UUID id) {
        return recruiterMapper.toDTO(recruiterTools.getRecruiterOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RecruiterDTO> getLinkedForCurrentUser() {
        return userTools
                .findCurrentUserFetchingRecruiter()
                .map(u -> u.getRecruiter())
                .map(recruiterMapper::toDTO);
    }

    @Override
    @Transactional
    public RecruiterDTO create(AddRecruiterReq addRecruiterReq) {
        RecruiterEnt recruiterEnt = recruiterTools.findOrCreateRecruiter(addRecruiterReq);
        RecruiterDTO recruiterDTO = recruiterMapper.toDTO(recruiterEnt);
        log.info(
                "User {} created or resolved recruiter id={}",
                securityHelper.getCurrentUsername(),
                recruiterEnt.getId());
        return recruiterDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<RecruiterDTO> getAllByFilter(
            Pageable pageable,
            FilterRecruiterReq filterRecruiterReq
    ) {
        Page<RecruiterEnt> page = recruiterRepo.findAll(
                RecruiterSpecifications.byFilters(filterRecruiterReq),
                pageable);

        return new PageResponse<>(
                page.getContent().stream().map(recruiterMapper::toDTO).toList(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Override
    @Transactional
    public RecruiterDTO update(
            UUID id,
            UpdateRecruiterReq updateRecruiterReq
    ) {
        RecruiterEnt recruiterEnt = recruiterTools.getRecruiterOrThrow(id);

        recruiterMapper.updateEntityFromDto(updateRecruiterReq, recruiterEnt);

        RecruiterDTO recruiterDTO = recruiterMapper.toDTO(recruiterEnt);

        log.info("User {} updated recruiter id={}", securityHelper.getCurrentUsername(), id);

        return recruiterDTO;
    }

    @Override
    @Transactional
    public RecruiterDTO patch(UUID id, PatchRecruiterReq patchRecruiterReq) {
        accountAccessHelper.requireRecruiterOwnsProfile(id);
        RecruiterEnt recruiterEnt = recruiterTools.getRecruiterOrThrow(id);

        recruiterMapper.patchEntityFromDto(patchRecruiterReq, recruiterEnt);

        RecruiterDTO recruiterDTO = recruiterMapper.toDTO(recruiterEnt);

        log.info("User {} patched recruiter id={}", securityHelper.getCurrentUsername(), id);

        return recruiterDTO;
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        recruiterTools.getRecruiterOrThrow(id);
        recruiterDeletionService.deleteRecruiterCascade(id);
        log.info("User {} deleted recruiter id={}", securityHelper.getCurrentUsername(), id);
    }
}
