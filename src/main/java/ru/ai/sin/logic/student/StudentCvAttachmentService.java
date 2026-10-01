package ru.ai.sin.logic.student;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.logic.company.CompanyEnt;
import ru.ai.sin.logic.company.CompanyRepo;
import ru.ai.sin.logic.education.EducationEnt;
import ru.ai.sin.logic.education.EducationRepo;
import ru.ai.sin.logic.experience.ExperienceEnt;
import ru.ai.sin.logic.experience.ExperienceRepo;
import ru.ai.sin.logic.institution.InstitutionEnt;
import ru.ai.sin.logic.institution.InstitutionRepo;
import ru.ai.sin.logic.student.dto.CreateStudentExperienceReq;
import ru.ai.sin.logic.student.dto.CreateStudentInstitutionReq;

import java.util.List;

/**
 * Создание связанных записей опыта и образования для студента (админское расширенное создание и саморегистрация).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StudentCvAttachmentService {

    private final CompanyRepo companyRepo;
    private final ExperienceRepo experienceRepo;
    private final EducationRepo educationRepo;
    private final InstitutionRepo institutionRepo;

    public void attachExperiences(StudentEnt studentEnt, List<CreateStudentExperienceReq> experienceItems) {
        if (experienceItems == null || experienceItems.isEmpty()) {
            return;
        }

        for (CreateStudentExperienceReq item : experienceItems) {
            if (item == null) {
                continue;
            }
            if (item.startDate() == null) {
                throw new BadRequestException("Experience startDate is required");
            }
            if (item.endDate() != null && item.endDate().isBefore(item.startDate())) {
                throw new BadRequestException("Дата окончания работы не может быть раньше даты начала");
            }

            CompanyEnt companyEnt = resolveCompany(item);

            ExperienceEnt experienceEnt = new ExperienceEnt();
            experienceEnt.setCompany(companyEnt);
            experienceEnt.setStudent(studentEnt);
            experienceEnt.setPosition(item.position());
            experienceEnt.setAdditionalInfo(item.additionalInfo());
            experienceEnt.setStartDate(item.startDate());
            experienceEnt.setEndDate(item.endDate());

            try {
                experienceRepo.save(experienceEnt);
            } catch (DataIntegrityViolationException ex) {
                log.warn("Experience already exists for student {}", studentEnt.getId());
                throw new BadRequestException("Experience already exists");
            }
        }
    }

    public void attachInstitutions(StudentEnt studentEnt, List<CreateStudentInstitutionReq> institutionItems) {
        if (institutionItems == null || institutionItems.isEmpty()) {
            return;
        }

        for (CreateStudentInstitutionReq item : institutionItems) {
            if (item == null) {
                continue;
            }
            if (item.endYear() < item.startYear()) {
                throw new BadRequestException("Год окончания обучения не может быть раньше года начала");
            }

            EducationEnt educationEnt = resolveEducation(item);

            InstitutionEnt institutionEnt = new InstitutionEnt();
            institutionEnt.setEducation(educationEnt);
            institutionEnt.setStudent(studentEnt);
            institutionEnt.setStartYear(item.startYear());
            institutionEnt.setEndYear(item.endYear());

            try {
                institutionRepo.save(institutionEnt);
            } catch (DataIntegrityViolationException ex) {
                log.warn("Institution already exists for student {}", studentEnt.getId());
                throw new BadRequestException("Institution already exists");
            }
        }
    }

    private CompanyEnt resolveCompany(CreateStudentExperienceReq item) {
        if (item.companyId() != null) {
            return companyRepo.findById(item.companyId())
                    .orElseThrow(() -> new BadRequestException("Company not found: " + item.companyId()));
        }
        if (isBlank(item.companyName())) {
            throw new BadRequestException("Experience companyId or companyName is required");
        }

        String companyName = normalize(item.companyName());
        return companyRepo.findFirstByNameIgnoreCase(companyName)
                .orElseGet(() -> companyRepo.save(new CompanyEnt(companyName)));
    }

    private EducationEnt resolveEducation(CreateStudentInstitutionReq item) {
        if (item.educationId() != null) {
            return educationRepo.findById(item.educationId())
                    .orElseThrow(() -> new BadRequestException("Education not found: " + item.educationId()));
        }

        if (isBlank(item.institution())) {
            throw new BadRequestException("Institution educationId or institution name is required");
        }
        if (isBlank(item.webUrl())) {
            throw new BadRequestException("Institution webUrl is required for new education");
        }

        String institutionName = normalize(item.institution());
        EducationEnt existing = educationRepo.findFirstByInstitutionIgnoreCase(institutionName).orElse(null);
        if (existing != null) {
            return existing;
        }

        EducationEnt educationEnt = new EducationEnt();
        educationEnt.setInstitution(institutionName);
        educationEnt.setAdditionalInfo(item.additionalInfo());
        educationEnt.setWebUrl(normalize(item.webUrl()));
        return educationRepo.save(educationEnt);
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
