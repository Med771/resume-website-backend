package ru.ai.sin.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.logic.company.CompanyEnt;
import ru.ai.sin.logic.company.CompanyRepo;
import ru.ai.sin.logic.education.EducationEnt;
import ru.ai.sin.logic.education.EducationRepo;
import ru.ai.sin.logic.experience.ExperienceEnt;
import ru.ai.sin.logic.experience.ExperienceRepo;
import ru.ai.sin.logic.institution.InstitutionEnt;
import ru.ai.sin.logic.institution.InstitutionRepo;
import ru.ai.sin.logic.portfolio.PortfolioEnt;
import ru.ai.sin.logic.portfolio.PortfolioRepo;
import ru.ai.sin.logic.skill.SkillEnt;
import ru.ai.sin.logic.skill.SkillRepo;
import ru.ai.sin.logic.speciality.SpecialityEnt;
import ru.ai.sin.logic.speciality.SpecialityRepo;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.student.StudentSpecifications;
import ru.ai.sin.logic.student.dto.FilterStudentReq;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.models.embeddables.UserInformation;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;
import ru.ai.sin.models.enums.JobSearchStatus;
import ru.ai.sin.models.enums.RoleEnum;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CatalogAudienceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private StudentRepo studentRepo;
    @Autowired
    private UserRepo userRepo;
    @Autowired
    private SkillRepo skillRepo;
    @Autowired
    private SpecialityRepo specialityRepo;
    @Autowired
    private CompanyRepo companyRepo;
    @Autowired
    private ExperienceRepo experienceRepo;
    @Autowired
    private EducationRepo educationRepo;
    @Autowired
    private InstitutionRepo institutionRepo;
    @Autowired
    private PortfolioRepo portfolioRepo;

    @Test
    @Transactional
    void catalogSearch_matchesLastOrFirstName_notMiddleOrBio() {
        StudentEnt byLast = ready("Анна", "Моисеева", "Сергеевна", "обычный текст анкеты длиннее тридцати");
        StudentEnt byFirst = ready("Моисей", "Петров", "Иванович", "обычный текст анкеты длиннее тридцати");
        StudentEnt byMiddle = ready("Иван", "Сидоров", "Моисеевич", "обычный текст анкеты длиннее тридцати");
        StudentEnt byBio = ready("Ольга", "Смирнова", "Ивановна", "мои проекты описаны здесь достаточно длинно");

        List<UUID> found = catalog("мои");

        assertThat(found).contains(byLast.getId(), byFirst.getId());
        assertThat(found).doesNotContain(byMiddle.getId(), byBio.getId());
    }

    @Test
    @Transactional
    void catalog_hidesIncompleteAndUnverifiedEmail() {
        StudentEnt complete = ready("Анна", "Полная", null, "Описание анкеты длиннее тридцати символов.");
        StudentEnt incomplete = ready("Анна", "Пустая", null, "Описание анкеты длиннее тридцати символов.");
        incomplete.setImagePath(null);
        studentRepo.saveAndFlush(incomplete);
        StudentEnt unverified = ready("Анна", "Почта", null, "Описание анкеты длиннее тридцати символов.");
        UserEnt user = userRepo.findByStudent_Id(unverified.getId()).orElseThrow();
        user.setEmailVerified(false);
        userRepo.saveAndFlush(user);

        List<UUID> found = catalog(null);

        assertThat(found).contains(complete.getId());
        assertThat(found).doesNotContain(incomplete.getId(), unverified.getId());
    }

    @Test
    @Transactional
    void catalog_defaultOrder_searchStatusThenDesiredBusyness() {
        StudentEnt late = ready("А", "Я", null, "Описание анкеты длиннее тридцати символов.");
        late.setJobSearchStatus(JobSearchStatus.NOT_LOOKING);
        late.setDesiredBusyness(BusynessEnum.EMPLOYED);
        StudentEnt lookingFree = ready("Б", "Б", null, "Описание анкеты длиннее тридцати символов.");
        lookingFree.setJobSearchStatus(JobSearchStatus.LOOKING);
        lookingFree.setDesiredBusyness(BusynessEnum.FREE);
        StudentEnt lookingEmployed = ready("В", "В", null, "Описание анкеты длиннее тридцати символов.");
        lookingEmployed.setJobSearchStatus(JobSearchStatus.LOOKING);
        lookingEmployed.setDesiredBusyness(BusynessEnum.EMPLOYED);
        studentRepo.saveAll(List.of(late, lookingFree, lookingEmployed));
        studentRepo.flush();

        List<UUID> found = catalog(null);

        assertThat(found).containsSubsequence(
                lookingEmployed.getId(),
                lookingFree.getId(),
                late.getId());
    }

    @Test
    @Transactional
    void catalog_filtersByCityAndDesiredBusyness() {
        StudentEnt match = ready("Г", "Г", null, "Описание анкеты длиннее тридцати символов.");
        StudentEnt other = ready("Д", "Д", null, "Описание анкеты длиннее тридцати символов.");
        var kazan = ensureCity("Kazan-" + UUID.randomUUID());
        other.setCityCatalog(kazan);
        other.setCity(kazan.getName());
        other.setDesiredBusyness(BusynessEnum.FREELANCE);
        studentRepo.saveAndFlush(other);

        assertThat(catalog(null, List.of(match.getCityCatalog().getId()), null))
                .contains(match.getId())
                .doesNotContain(other.getId());
        assertThat(catalog(null, null, Set.of(BusynessEnum.EMPLOYED)))
                .contains(match.getId())
                .doesNotContain(other.getId());
        assertThat(catalog(null)).contains(match.getId(), other.getId());
    }

    private List<UUID> catalog(String findString) {
        return catalog(findString, null, null);
    }

    private List<UUID> catalog(String findString, List<Long> cityIds, Set<BusynessEnum> desiredBusyness) {
        FilterStudentReq filter = new FilterStudentReq(
                findString, null, null, null, null, null, null, null, null, null, null, null,
                cityIds, desiredBusyness);
        return StudentSpecifications.findPage(
                        studentRepo,
                        PageRequest.of(0, 200),
                        filter,
                        false,
                        false,
                        true,
                        null)
                .map(StudentEnt::getId)
                .toList();
    }

    private StudentEnt ready(String firstName, String lastName, String middleName, String bio) {
        String token = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        SpecialityEnt speciality = new SpecialityEnt();
        speciality.setName("Spec-" + token);
        speciality = specialityRepo.save(speciality);

        List<SkillEnt> skills = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            SkillEnt skill = new SkillEnt();
            skill.setName("Skill-" + token + "-" + i);
            skills.add(skillRepo.save(skill));
        }

        StudentEnt student = new StudentEnt();
        student.setBirthDate(LocalDate.of(2002, 3, 4));
        student.setCourse(CourseEnum.FIRST);
        student.setBusyness(BusynessEnum.FREE);
        student.setCityCatalog(ensureCity("Moscow"));
        student.setCity("Moscow");
        student.setImagePath("photo-" + token + ".jpg");
        student.setBio(bio);
        student.setCatalogVisible(true);
        student.setSpeciality(speciality);
        student.setUserInformation(new UserInformation(firstName, lastName, token + "@catalog.test"));
        student.setMiddleName(middleName);
        student.setSkills(new HashSet<>(skills));
        student.setJobSearchStatus(JobSearchStatus.LOOKING);
        student.setDesiredBusyness(BusynessEnum.EMPLOYED);
        student = studentRepo.saveAndFlush(student);

        CompanyEnt company = new CompanyEnt();
        company.setName("Co-" + token);
        company = companyRepo.save(company);
        ExperienceEnt experience = new ExperienceEnt();
        experience.setCompany(company);
        experience.setStudent(student);
        experience.setPosition("Dev");
        experience.setStartDate(LocalDate.of(2020, 1, 1));
        experienceRepo.save(experience);

        EducationEnt education = new EducationEnt();
        education.setInstitution("Uni-" + token);
        education.setWebUrl("https://university.example/" + token);
        education = educationRepo.save(education);
        InstitutionEnt institution = new InstitutionEnt();
        institution.setEducation(education);
        institution.setStudent(student);
        institution.setStartYear(2020);
        institution.setEndYear(2024);
        institutionRepo.save(institution);

        PortfolioEnt portfolio = new PortfolioEnt();
        portfolio.setName("Site");
        portfolio.setLink("https://example.com/" + token);
        portfolio.setStudent(student);
        portfolioRepo.save(portfolio);

        UserEnt user = new UserEnt(RoleEnum.STUDENT, lastName + " " + firstName, "u_" + token, "hash");
        user.setStudent(student);
        user.setAccountStatus(AccountStatus.APPROVED);
        user.setEmailVerified(true);
        userRepo.saveAndFlush(user);
        return student;
    }
}
