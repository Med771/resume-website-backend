package ru.ai.sin.logic.recruiter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import org.springframework.stereotype.Service;

import org.springframework.transaction.annotation.Transactional;

import org.springframework.web.multipart.MultipartFile;

import ru.ai.sin.models.PageResponse;

import ru.ai.sin.exception.models.BadRequestException;

import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.FileHelper;
import ru.ai.sin.helper.SecurityHelper;

import ru.ai.sin.logic.recruiter.dto.*;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;

import ru.ai.sin.tools.RecruiterTools;
import ru.ai.sin.tools.UserTools;

import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    private final UserRepo userRepo;
    private final FileHelper fileHelper;

    private final SecurityHelper securityHelper;
    private final AccountAccessHelper accountAccessHelper;

    private final RecruiterDeletionService recruiterDeletionService;

    @Override
    @Transactional(readOnly = true)
    public RecruiterDTO getById(UUID id) {
        RecruiterEnt recruiter = recruiterTools.getRecruiterOrThrow(id);
        return recruiterMapper.toDTO(recruiter, usernameOf(recruiter.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RecruiterDTO> getLinkedForCurrentUser() {
        return userTools
                .findCurrentUserFetchingRecruiter()
                .filter(user -> user.getRecruiter() != null)
                .map(user -> recruiterMapper.toDTO(user.getRecruiter(), user.getUsername()));
    }

    @Override
    @Transactional
    public RecruiterDTO create(AddRecruiterReq addRecruiterReq) {
        RecruiterEnt recruiterEnt = recruiterTools.findOrCreateRecruiter(addRecruiterReq);
        RecruiterDTO recruiterDTO = toDto(recruiterEnt);
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

        Map<UUID, String> usernames = usernamesOf(page.getContent());
        return new PageResponse<>(
                page.getContent().stream()
                        .map(recruiter -> recruiterMapper.toDTO(recruiter, usernames.get(recruiter.getId())))
                        .toList(),
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

        RecruiterDTO recruiterDTO = toDto(recruiterEnt);

        log.info("User {} updated recruiter id={}", securityHelper.getCurrentUsername(), id);

        return recruiterDTO;
    }

    @Override
    @Transactional
    public RecruiterDTO patch(UUID id, PatchRecruiterReq patchRecruiterReq) {
        accountAccessHelper.requireRecruiterOwnsProfile(id);
        RecruiterEnt recruiterEnt = recruiterTools.getRecruiterOrThrow(id);

        recruiterMapper.patchEntityFromDto(patchRecruiterReq, recruiterEnt);

        RecruiterDTO recruiterDTO = toDto(recruiterEnt);

        log.info("User {} patched recruiter id={}", securityHelper.getCurrentUsername(), id);

        return recruiterDTO;
    }

    @Override
    @Transactional
    public void setPhoto(UUID id, MultipartFile file) {
        accountAccessHelper.requireRecruiterOwnsProfile(id);
        fileHelper.validateMultipart(file);
        RecruiterEnt recruiter = recruiterTools.getRecruiterOrThrow(id);
        String filePath = fileHelper.saveFile(file, recruiter.getId().toString());
        if (filePath == null) {
            throw new BadRequestException("Failed to save file");
        }
        String previous = recruiter.getImagePath();
        recruiter.setImagePath(filePath);
        if (previous != null && !previous.isBlank() && !previous.equals(filePath)) {
            deleteStoredFile(previous);
        }
    }

    @Override
    @Transactional
    public void deletePhoto(UUID id) {
        accountAccessHelper.requireRecruiterOwnsProfile(id);
        RecruiterEnt recruiter = recruiterTools.getRecruiterOrThrow(id);
        String imagePath = recruiter.getImagePath();
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }
        deleteStoredFile(imagePath);
        recruiter.setImagePath(null);
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        recruiterTools.getRecruiterOrThrow(id);
        recruiterDeletionService.deleteRecruiterCascade(id);
        log.info("User {} deleted recruiter id={}", securityHelper.getCurrentUsername(), id);
    }

    private RecruiterDTO toDto(RecruiterEnt recruiter) {
        return recruiterMapper.toDTO(recruiter, usernameOf(recruiter.getId()));
    }

    private String usernameOf(UUID recruiterId) {
        if (recruiterId == null) {
            return null;
        }
        return userRepo.findByRecruiter_Id(recruiterId).map(UserEnt::getUsername).orElse(null);
    }

    private Map<UUID, String> usernamesOf(List<RecruiterEnt> recruiters) {
        List<UUID> ids = recruiters.stream().map(RecruiterEnt::getId).filter(id -> id != null).toList();
        if (ids.isEmpty()) {
            return Map.of();
        }
        Map<UUID, String> usernames = new HashMap<>();
        for (UserEnt user : userRepo.findByRecruiter_IdIn(ids)) {
            if (user.getRecruiter() != null && user.getRecruiter().getId() != null) {
                usernames.put(user.getRecruiter().getId(), user.getUsername());
            }
        }
        return usernames;
    }

    private void deleteStoredFile(String imagePath) {
        try {
            fileHelper.deleteFile(imagePath);
        } catch (FileNotFoundException ignored) {
            // файла уже нет
        }
    }
}
