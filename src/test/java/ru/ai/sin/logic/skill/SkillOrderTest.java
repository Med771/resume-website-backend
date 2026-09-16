package ru.ai.sin.logic.skill;

import org.junit.jupiter.api.Test;
import ru.ai.sin.models.embeddables.TimeStamped;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SkillOrderTest {

    @Test
    void byCreatedAtThenId_ordersOlderFirst() {
        LocalDateTime t1 = LocalDateTime.of(2024, 1, 1, 10, 0);
        LocalDateTime t2 = LocalDateTime.of(2024, 1, 2, 10, 0);
        SkillEnt newer = skill(1L, "A", t2);
        SkillEnt older = skill(9L, "Z", t1);

        List<SkillEnt> skills = new ArrayList<>(List.of(newer, older));
        skills.sort(SkillOrder.byCreatedAtThenId());

        assertThat(skills).containsExactly(older, newer);
    }

    @Test
    void byCreatedAtThenId_sameCreatedAt_ordersById() {
        LocalDateTime t = LocalDateTime.of(2024, 1, 1, 10, 0);
        SkillEnt highId = skill(20L, "First alphabetically", t);
        SkillEnt lowId = skill(3L, "Zebra", t);

        List<SkillEnt> skills = new ArrayList<>(List.of(highId, lowId));
        skills.sort(SkillOrder.byCreatedAtThenId());

        assertThat(skills).containsExactly(lowId, highId);
    }

    private static SkillEnt skill(Long id, String name, LocalDateTime createdAt) {
        SkillEnt ent = new SkillEnt(name);
        ent.setId(id);
        ent.setTimestamps(new TimeStamped(createdAt, createdAt));
        return ent;
    }
}
