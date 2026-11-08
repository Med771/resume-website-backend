package ru.ai.sin.logic.analytics;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.experience.dto.AddExperienceReq;
import ru.ai.sin.logic.experience.dto.UpdateExperienceReq;
import ru.ai.sin.logic.institution.dto.AddInstitutionReq;
import ru.ai.sin.logic.institution.dto.UpdateInstitutionReq;
import ru.ai.sin.logic.portfolio.dto.AddPortfolioReq;
import ru.ai.sin.logic.student.dto.PatchStudentMeReq;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.models.enums.AnalyticsEventType;
import ru.ai.sin.models.enums.EducationType;
import ru.ai.sin.models.enums.ExperienceType;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.tools.UserTools;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StudentProfileChangeRecorder {

    private final SecurityHelper securityHelper;
    private final UserTools userTools;
    private final AnalyticsEventWriter analyticsEventWriter;

    public void record(
            UUID studentId,
            String path,
            String section,
            String action,
            Long itemId,
            List<String> fields
    ) {
        Optional<String> role = securityHelper.getCurrentRoleOptional();
        if (role.isEmpty() || !RoleEnum.STUDENT.getRole().equals(role.get())) {
            return;
        }
        Optional<UserEnt> current = userTools.findCurrentUserFetchingLinks();
        if (current.isEmpty()) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("studentId", studentId);
        payload.put("section", section);
        payload.put("action", action);
        payload.put("itemId", itemId);
        payload.put("fields", fields == null ? List.of() : fields);
        analyticsEventWriter.record(
                AnalyticsEventType.STUDENT_PROFILE_CHANGED.getCode(),
                path,
                current.get().getId(),
                payload);
    }

    public static List<String> cardFields(PatchStudentMeReq req) {
        List<String> fields = new ArrayList<>();
        if (req.city() != null) {
            fields.add("city");
        }
        if (req.hhLink() != null) {
            fields.add("hhLink");
        }
        if (req.birthDate() != null) {
            fields.add("birthDate");
        }
        if (req.bio() != null) {
            fields.add("bio");
        }
        if (req.course() != null) {
            fields.add("course");
        }
        if (req.busyness() != null) {
            fields.add("busyness");
        }
        if (req.desiredBusyness() != null) {
            fields.add("desiredBusyness");
        }
        if (req.jobSearchStatus() != null) {
            fields.add("jobSearchStatus");
        }
        if (req.firstName() != null) {
            fields.add("firstName");
        }
        if (req.lastName() != null) {
            fields.add("lastName");
        }
        if (req.middleName() != null) {
            fields.add("middleName");
        }
        if (req.gender() != null) {
            fields.add("gender");
        }
        if (req.email() != null) {
            fields.add("email");
        }
        if (req.phoneNumber() != null) {
            fields.add("phoneNumber");
        }
        if (req.telegramUsername() != null) {
            fields.add("telegramUsername");
        }
        if (req.specialityId() != null) {
            fields.add("specialityId");
        }
        if (req.skillsIds() != null) {
            fields.add("skillsIds");
        }
        if (req.publicProfileConsent() != null) {
            fields.add("publicProfileConsent");
        }
        return fields;
    }

    public static List<String> experienceFields(AddExperienceReq req) {
        return experienceFields(
                req.position(), req.additionalInfo(), req.startDate(), req.endDate(), req.experienceType());
    }

    public static List<String> experienceFields(UpdateExperienceReq req) {
        return experienceFields(
                req.position(), req.additionalInfo(), req.startDate(), req.endDate(), req.experienceType());
    }

    public static List<String> institutionFields(AddInstitutionReq req) {
        return institutionFields(req.educationType(), req.direction(), req.educationLevel());
    }

    public static List<String> institutionFields(UpdateInstitutionReq req) {
        return institutionFields(req.educationType(), req.direction(), req.educationLevel());
    }

    public static List<String> portfolioFields(AddPortfolioReq req) {
        List<String> fields = new ArrayList<>();
        if (req.name() != null) {
            fields.add("name");
        }
        if (req.link() != null) {
            fields.add("link");
        }
        if (req.additionalInfo() != null) {
            fields.add("additionalInfo");
        }
        return fields;
    }

    private static List<String> experienceFields(
            String position,
            String additionalInfo,
            LocalDate startDate,
            LocalDate endDate,
            ExperienceType experienceType
    ) {
        List<String> fields = new ArrayList<>();
        fields.add("companyId");
        if (position != null) {
            fields.add("position");
        }
        if (additionalInfo != null) {
            fields.add("additionalInfo");
        }
        if (startDate != null) {
            fields.add("startDate");
        }
        if (endDate != null) {
            fields.add("endDate");
        }
        if (experienceType != null) {
            fields.add("experienceType");
        }
        return fields;
    }

    private static List<String> institutionFields(
            EducationType educationType,
            String direction,
            String educationLevel
    ) {
        List<String> fields = new ArrayList<>();
        fields.add("educationId");
        fields.add("startYear");
        fields.add("endYear");
        if (educationType != null) {
            fields.add("educationType");
        }
        if (direction != null) {
            fields.add("direction");
        }
        if (educationLevel != null) {
            fields.add("educationLevel");
        }
        return fields;
    }
}
