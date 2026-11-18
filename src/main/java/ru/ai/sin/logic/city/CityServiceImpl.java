package ru.ai.sin.logic.city;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.city.dto.AddCityReq;
import ru.ai.sin.logic.city.dto.CityDTO;
import ru.ai.sin.logic.city.dto.FilterCityReq;
import ru.ai.sin.logic.city.dto.UpdateCityReq;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.models.PageResponse;

@Slf4j
@Service
@RequiredArgsConstructor
public class CityServiceImpl implements CityService {

    private final CityRepo cityRepo;
    private final CityMapper cityMapper;
    private final StudentRepo studentRepo;
    private final SecurityHelper securityHelper;

    @Override
    @Transactional(readOnly = true)
    public CityDTO getById(long id) {
        return cityMapper.toDTO(cityRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Город не найден")));
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<CityDTO> getAllByFilter(Pageable pageable, FilterCityReq filter) {
        Page<CityEnt> page = cityRepo.findAll(CitySpecifications.byFilters(filter), pageable);
        return new PageResponse<>(
                page.getContent().stream().map(cityMapper::toDTO).toList(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    @Override
    @Transactional
    public CityDTO create(AddCityReq addCityReq) {
        String name = addCityReq.name().trim();
        if (cityRepo.findFirstByNameIgnoreCase(name).isPresent()) {
            throw new BadRequestException("Город уже есть: " + name);
        }
        CityEnt city = cityMapper.toEntity(addCityReq);
        city.setName(name);
        try {
            city = cityRepo.save(city);
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("Город уже есть: " + name);
        }
        CityDTO dto = cityMapper.toDTO(city);
        log.info("User: {}, created a city: {}", securityHelper.getCurrentUsername(), dto);
        return dto;
    }

    @Override
    @Transactional
    public CityDTO update(long id, UpdateCityReq updateCityReq) {
        CityEnt city = cityRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Город не найден"));
        String name = updateCityReq.name().trim();
        boolean taken = cityRepo.findFirstByNameIgnoreCase(name)
                .filter(other -> !other.getId().equals(id))
                .isPresent();
        if (taken) {
            throw new BadRequestException("Город уже есть: " + name);
        }
        city.setName(name);
        studentRepo.syncCityName(id, name);
        CityDTO dto = cityMapper.toDTO(city);
        log.info("User: {}, updated a city: {}", securityHelper.getCurrentUsername(), dto);
        return dto;
    }

    @Override
    @Transactional
    public void deleteById(long id) {
        CityEnt city = cityRepo.findById(id)
                .orElseThrow(() -> new NotFoundException("Город не найден"));
        studentRepo.detachCity(id);
        cityRepo.delete(city);
        log.info("User: {}, deleted a city: {}", securityHelper.getCurrentUsername(), id);
    }
}
