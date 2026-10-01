package ru.ai.sin.logic.student;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.logic.skill.SkillEnt;
import ru.ai.sin.logic.skill.SkillRepo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentSkillsMutatorTest {

    @Mock
    private SkillRepo skillRepo;

    @InjectMocks
    private StudentSkillsMutator mutator;

    @Test
    void replaceSkills_clearsImmutableCollectionWithoutException() {
        SkillEnt skill = new SkillEnt();
        skill.setId(1L);
        skill.setName("Java");
        when(skillRepo.findAllByIdIn(Set.of(1L))).thenReturn(Set.of(skill));

        StudentEnt student = new StudentEnt();
        student.setSkills(Set.of());

        mutator.replaceSkills(student, List.of(1L));

        assertThat(student.getSkills()).hasSize(1);
        assertThat(student.getSkills().iterator().next().getId()).isEqualTo(1L);
    }

    @Test
    void replaceSkills_emptyList_clearsExistingSkills() {
        SkillEnt skill = new SkillEnt();
        skill.setId(2L);
        StudentEnt student = new StudentEnt();
        student.setSkills(new HashSet<>(Set.of(skill)));

        mutator.replaceSkills(student, List.of());

        assertThat(student.getSkills()).isEmpty();
    }
}
