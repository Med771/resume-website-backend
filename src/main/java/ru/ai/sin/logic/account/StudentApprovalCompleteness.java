package ru.ai.sin.logic.account;

import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.models.embeddables.UserInformation;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class StudentApprovalCompleteness {

    static final int MIN_SKILLS = 5;
    static final int MIN_BIO_LENGTH = 30;

    private StudentApprovalCompleteness() {
    }

    static List<String> problems(StudentEnt student) {
        List<String> problems = new ArrayList<>();
        if (blank(student.getImagePath())) {
            problems.add("нет фото");
        }
        if (empty(student.getPortfolio())) {
            problems.add("нет портфолио");
        }
        if (empty(student.getCompanies())) {
            problems.add("нет опыта");
        }
        if (empty(student.getEducation())) {
            problems.add("нет образования");
        }
        if (bioTooShort(student.getBio())) {
            problems.add("описание короче 30 символов");
        }
        UserInformation name = student.getUserInformation();
        if (name == null || blank(name.getFirstName())) {
            problems.add("нет имени");
        }
        if (name == null || blank(name.getLastName())) {
            problems.add("нет фамилии");
        }
        if (student.getSpeciality() == null) {
            problems.add("нет специальности");
        }
        if (student.getCourse() == null) {
            problems.add("нет курса");
        }
        if (student.getSkills() == null || student.getSkills().size() < MIN_SKILLS) {
            problems.add("навыков меньше 5");
        }
        if (student.getBusyness() == null) {
            problems.add("нет типа занятости");
        }
        if (blank(student.getCity())) {
            problems.add("нет города");
        }
        return problems;
    }

    private static boolean bioTooShort(String bio) {
        return bio == null || bio.trim().length() < MIN_BIO_LENGTH;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static boolean empty(Collection<?> items) {
        return items == null || items.isEmpty();
    }
}
