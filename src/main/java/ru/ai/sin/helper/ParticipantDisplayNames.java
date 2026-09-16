package ru.ai.sin.helper;

import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.models.embeddables.UserInformation;

public final class ParticipantDisplayNames {

    private ParticipantDisplayNames() {}

    public static String recruiter(RecruiterEnt recruiter) {
        if (recruiter == null) {
            return null;
        }
        UserInformation info = recruiter.getUserInformation();
        String person = formatPerson(info != null ? info.getFirstName() : null, info != null ? info.getLastName() : null);
        String company = recruiter.getCompanyName();
        if (person != null && company != null) {
            return person + " (" + company + ")";
        }
        if (company != null) {
            return company;
        }
        return person;
    }

    public static String student(StudentEnt student) {
        if (student == null) {
            return null;
        }
        UserInformation info = student.getUserInformation();
        return formatPerson(info != null ? info.getFirstName() : null, info != null ? info.getLastName() : null);
    }

    /** ФИО в порядке «Фамилия Имя Отчество», как в {@code users.name}. */
    public static String fromFio(String lastName, String firstName, String middleName) {
        StringBuilder sb = new StringBuilder();
        appendPart(sb, lastName);
        appendPart(sb, firstName);
        appendPart(sb, middleName);
        return sb.isEmpty() ? null : sb.toString();
    }

    private static void appendPart(StringBuilder sb, String part) {
        if (part == null || part.isBlank()) {
            return;
        }
        if (!sb.isEmpty()) {
            sb.append(' ');
        }
        sb.append(part.trim());
    }

    private static String formatPerson(String first, String last) {
        String f = first != null ? first.trim() : "";
        String l = last != null ? last.trim() : "";
        String combined = (f + " " + l).trim();
        return combined.isEmpty() ? null : combined;
    }
}
