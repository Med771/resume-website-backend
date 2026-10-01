package ru.ai.sin.tools;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.ai.sin.logic.education.EducationEnt;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.logic.education.EducationRepo;

@Component
@RequiredArgsConstructor
public class EducationTools {

    private final EducationRepo educationRepo;

    @Transactional
    public EducationEnt getEducationOrThrow(long id) {
        return educationRepo.findById(id).orElseThrow(
                () -> new NotFoundException("Failed to find education with id" + id)
        );
    }
}
