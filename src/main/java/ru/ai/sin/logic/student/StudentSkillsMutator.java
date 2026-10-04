package ru.ai.sin.logic.student;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.logic.skill.SkillEnt;
import ru.ai.sin.logic.skill.SkillRepo;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Безопасная замена ManyToMany skills у студента: Hibernate требует изменяемую коллекцию
 * (нельзя присваивать результат {@code Set.of()} / immutable set из репозитория).
 */
@Component
@RequiredArgsConstructor
public class StudentSkillsMutator {

    private final SkillRepo skillRepo;

    public void replaceSkills(StudentEnt student, List<Long> skillsIds) {
        Set<SkillEnt> resolved = resolveSkillsByIdsOrThrow(skillsIds);
        Set<SkillEnt> target = student.getSkills();
        if (target == null) {
            student.setSkills(new HashSet<>(resolved));
            return;
        }
        if (isHibernateCollection(target)) {
            target.clear();
            target.addAll(resolved);
            return;
        }
        try {
            target.clear();
            target.addAll(resolved);
        } catch (UnsupportedOperationException ex) {
            student.setSkills(new HashSet<>(resolved));
        }
    }

    public Set<SkillEnt> resolveSkillsByIdsOrThrow(List<Long> skillsIds) {
        if (skillsIds == null || skillsIds.isEmpty()) {
            return new HashSet<>();
        }

        Set<Long> uniqueSkillIds = new HashSet<>(skillsIds);
        Set<SkillEnt> skillEntSet = new HashSet<>(skillRepo.findAllByIdIn(uniqueSkillIds));
        if (skillEntSet.size() != uniqueSkillIds.size()) {
            throw new BadRequestException("Some skills were not found by ids");
        }
        return skillEntSet;
    }

    private static boolean isHibernateCollection(Collection<?> collection) {
        return collection.getClass().getName().startsWith("org.hibernate.collection");
    }
}
