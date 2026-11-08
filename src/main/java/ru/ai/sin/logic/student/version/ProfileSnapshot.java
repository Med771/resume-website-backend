package ru.ai.sin.logic.student.version;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;
import ru.ai.sin.models.enums.EducationType;
import ru.ai.sin.models.enums.ExperienceType;
import ru.ai.sin.models.enums.GenderEnum;
import ru.ai.sin.models.enums.JobSearchStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
public class ProfileSnapshot {

    private UUID studentId;
    private String city;
    private String hhLink;
    private LocalDate birthDate;
    private String bio;
    private String imagePath;
    private CourseEnum course;
    private BusynessEnum busyness;
    private BusynessEnum desiredBusyness;
    private JobSearchStatus jobSearchStatus;
    private String firstName;
    private String lastName;
    private String middleName;
    private GenderEnum gender;
    private String email;
    private String phoneNumber;
    private String telegramUsername;
    private Long specialityId;
    private String speciality;
    private boolean publicProfileConsent;
    private List<SkillSnap> skills = new ArrayList<>();
    private List<ExperienceSnap> experience = new ArrayList<>();
    private List<EducationSnap> education = new ArrayList<>();
    private List<PortfolioSnap> portfolio = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    public static class SkillSnap {
        private Long id;
        private String name;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class ExperienceSnap {
        private long id;
        private long companyId;
        private String companyWebsite;
        private String position;
        private String additionalInfo;
        private LocalDate startDate;
        private LocalDate endDate;
        private ExperienceType experienceType;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class EducationSnap {
        private long id;
        private long educationId;
        private int startYear;
        private int endYear;
        private EducationType educationType;
        private String direction;
        private String educationLevel;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    public static class PortfolioSnap {
        private long id;
        private String name;
        private String link;
        private String additionalInfo;
    }
}
