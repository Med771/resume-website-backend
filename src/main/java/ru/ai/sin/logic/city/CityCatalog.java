package ru.ai.sin.logic.city;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.logic.student.StudentEnt;

@Component
@RequiredArgsConstructor
public class CityCatalog {

    private final CityRepo cityRepo;

    public void assignIfPresent(StudentEnt student, Long cityId, String cityText) {
        if (cityId == null && (cityText == null || cityText.isBlank())) {
            return;
        }
        CityEnt city = require(cityId, cityText);
        student.setCityCatalog(city);
        student.setCity(city.getName());
    }

    public CityEnt require(Long cityId, String cityText) {
        if (cityId != null) {
            return cityRepo.findById(cityId)
                    .orElseThrow(() -> new BadRequestException("Город не найден"));
        }
        String name = cityText == null ? "" : cityText.trim();
        if (name.isEmpty()) {
            throw new BadRequestException("Город не найден в справочнике");
        }
        return cityRepo.findFirstByNameIgnoreCase(name)
                .orElseThrow(() -> new BadRequestException("Город не найден в справочнике"));
    }

    public Long idOfName(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return cityRepo.findFirstByNameIgnoreCase(name.trim()).map(CityEnt::getId).orElse(null);
    }

    public void restoreFromSnapshot(StudentEnt student, String name) {
        if (name == null || name.isBlank()) {
            student.setCity(null);
            student.setCityCatalog(null);
            return;
        }
        cityRepo.findFirstByNameIgnoreCase(name.trim()).ifPresentOrElse(city -> {
            student.setCityCatalog(city);
            student.setCity(city.getName());
        }, () -> {
            student.setCityCatalog(null);
            student.setCity(name.trim());
        });
    }
}
