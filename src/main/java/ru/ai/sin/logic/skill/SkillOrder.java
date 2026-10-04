package ru.ai.sin.logic.skill;

import java.time.LocalDateTime;
import java.util.Comparator;

public final class SkillOrder {

    private SkillOrder() {}

    public static Comparator<SkillEnt> byCreatedAtThenId() {
        return Comparator
                .comparing(SkillOrder::createdAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(SkillEnt::getId, Comparator.nullsLast(Long::compareTo));
    }

    private static LocalDateTime createdAt(SkillEnt skill) {
        if (skill == null || skill.getTimestamps() == null) {
            return null;
        }
        return skill.getTimestamps().getCreatedAt();
    }
}
