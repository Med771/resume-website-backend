package ru.ai.sin.logic.student;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class StudentProfileScoring {

    public static int computeProfileTextScore(StudentEnt student) {
        int n = len(student.getBio()) + len(student.getCity()) + len(student.getHhLink());
        if (student.getUserInformation() != null) {
            n += len(student.getUserInformation().getFirstName());
            n += len(student.getUserInformation().getLastName());
        }
        n += len(student.getMiddleName());
        return n;
    }

    public static void applyTo(StudentEnt student) {
        student.setProfileTextScore(computeProfileTextScore(student));
    }

    private static int len(String s) {
        return s == null ? 0 : s.length();
    }
}
