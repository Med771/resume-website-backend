package ru.ai.sin.tools;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.ai.sin.logic.company.dto.CompanyDTO;

import ru.ai.sin.logic.company.CompanyEnt;

import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.logic.company.CompanyMapper;
import ru.ai.sin.logic.company.CompanyRepo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class CompanyTools {

    private final CompanyRepo companyRepo;

    private final CompanyMapper companyMapper;

    private final ExperienceTools experienceTools;

    @Transactional(readOnly = true)
    public CompanyEnt getCompanyOrThrow(long id) {
        return companyRepo.findWithExperiencesById(id).orElseThrow(
                () -> new NotFoundException("Failed to find company with id " + id)
        );
    }

    @Transactional
    public CompanyDTO mapToDTO(CompanyEnt companyEnt) {
        List<Long> experienceIds = experienceTools.getExperienceIdsByCompanyId(companyEnt.getId());

        return companyMapper.toDTO(companyEnt, experienceIds);
    }

    @Transactional
    public CompanyDTO newObjMapToDTO(CompanyEnt companyEnt) {
        return companyMapper.toDTO(companyEnt, new ArrayList<>(0));
    }

    @Transactional
    public List<CompanyDTO> mapToDTOs(List<CompanyEnt> companyEntList) {
        Set<Long> companyIds = companyEntList.stream().map(CompanyEnt::getId).collect(Collectors.toSet());
        Map<Long, List<Long>> experiencesIds = experienceTools.getExperienceIdsByCompanyIds(companyIds);

        return companyEntList.stream()
                .map(companyEnt ->
                        companyMapper.toDTO(
                                companyEnt,
                                experiencesIds.getOrDefault(companyEnt.getId(), List.of()))
                )
                .toList();
    }
}
