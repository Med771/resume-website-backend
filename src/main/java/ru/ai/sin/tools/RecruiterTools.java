package ru.ai.sin.tools;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.ai.sin.logic.recruiter.dto.AddRecruiterReq;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.recruiter.RecruiterMapper;
import ru.ai.sin.logic.recruiter.RecruiterRepo;

import org.springframework.util.StringUtils;

import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecruiterTools {

    private final RecruiterRepo recruiterRepo;

    private final RecruiterMapper recruiterMapper;

    @Transactional(readOnly = true)
    public RecruiterEnt getRecruiterOrThrow(UUID recruiterId) {
        return recruiterRepo.findById(recruiterId).orElseThrow(
                () -> new NotFoundException("Failed to find recruiter by id " + recruiterId)
        );
    }

    @Transactional
    public RecruiterEnt findOrCreateRecruiter(AddRecruiterReq addRecruiterReq) {
        RecruiterEnt recruiterEnt = null;
        if (StringUtils.hasText(addRecruiterReq.email())) {
            recruiterEnt = recruiterRepo
                    .findByUserInformationEmail(addRecruiterReq.email().trim())
                    .orElse(null);
        }

        if (recruiterEnt == null) {
            recruiterEnt = recruiterMapper.toEntity(addRecruiterReq);

            try {
                recruiterEnt = recruiterRepo.save(recruiterEnt);
                log.info("Created new recruiter: {} with email: {}", recruiterEnt.getId(), addRecruiterReq.email());
            } catch (DataIntegrityViolationException ex) {
                log.warn("Recruiter already exists: {}, {}", addRecruiterReq.email(), addRecruiterReq.telegramUsername());
                throw new BadRequestException("Recruiter already exists: %s, %s"
                        .formatted(addRecruiterReq.email(), addRecruiterReq.telegramUsername()));
            }
        }

        return recruiterEnt;
    }
}
