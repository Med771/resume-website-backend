package ru.ai.sin.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.logic.skill.SkillEnt;
import ru.ai.sin.logic.skill.SkillRepo;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.models.embeddables.UserInformation;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class StudentSkillsOrderIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private SkillRepo skillRepo;

    @Autowired
    private StudentRepo studentRepo;

    @Test
    @Transactional
    void findSkillsByStudentId_ordersByCreatedAtThenId() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);

        SkillEnt zebra = skillRepo.save(new SkillEnt("Zebra-" + suffix));
        SkillEnt apple = skillRepo.save(new SkillEnt("Apple-" + suffix));
        SkillEnt sofa = skillRepo.save(new SkillEnt("Sofa-" + suffix));

        StudentEnt student = new StudentEnt();
        student.setBirthDate(LocalDate.of(2003, 1, 15));
        student.setCourse(CourseEnum.FIRST);
        student.setBusyness(BusynessEnum.FREE);
        student.setUserInformation(new UserInformation("Ivan", "Ivanov", "ivan-" + suffix + "@test.local"));
        student.setSkills(new HashSet<>(List.of(apple, zebra, sofa)));
        student = studentRepo.save(student);

        List<SkillEnt> skills = studentRepo.findSkillsByStudentId(student.getId());

        assertThat(skills).extracting(SkillEnt::getId)
                .containsExactly(zebra.getId(), apple.getId(), sofa.getId());
    }
}