package ru.ai.sin.tools;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.ai.sin.exception.models.NotFoundException;

import ru.ai.sin.logic.speciality.SpecialityEnt;
import ru.ai.sin.logic.speciality.SpecialityRepo;

@Component
@RequiredArgsConstructor
public class SpecialityTools {

    private final SpecialityRepo specialityRepo;

    @Transactional
    public SpecialityEnt getSpecialityOrThrow(long specialityId) {
        return specialityRepo.findById(specialityId).orElseThrow(
                () -> new NotFoundException("Failed to find special by id " + specialityId)
        );
    }
}
