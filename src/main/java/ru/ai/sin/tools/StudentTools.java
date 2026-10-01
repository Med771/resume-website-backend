package ru.ai.sin.tools;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.logic.skill.SkillMapper;
import ru.ai.sin.logic.skill.SkillOrder;

import ru.ai.sin.logic.student.dto.StudentCardDTO;
import ru.ai.sin.logic.student.dto.StudentDTO;
import ru.ai.sin.logic.student.StudentEnt;

import ru.ai.sin.logic.student.StudentMapper;
import ru.ai.sin.logic.student.StudentRepo;

import ru.ai.sin.exception.models.NotFoundException;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class StudentTools {

    private final StudentRepo studentRepo;

    private final StudentMapper studentMapper;
    private final SkillMapper skillMapper;

    @Transactional(readOnly = true)
    public StudentEnt getStudentOrThrow(UUID studentId) {
        return studentRepo.findById(studentId).orElseThrow(
                () -> new NotFoundException("Failed to find student by id " + studentId)
        );
    }

    public StudentDTO mapToDTO(StudentEnt studentEnt) {
        List<SkillDTO> skillDTOList = studentRepo.findSkillsByStudentId(studentEnt.getId())
                .stream()
                .sorted(SkillOrder.byCreatedAtThenId())
                .map(skillMapper::toDTO)
                .toList();

        return studentMapper.toDTO(studentEnt, skillDTOList);
    }

    @Transactional
    public StudentCardDTO mapToCardDTO(StudentEnt studentEnt) {
        List<SkillDTO> skillDTOList = studentRepo.findSkillsByStudentId(studentEnt.getId())
                .stream()
                .sorted(SkillOrder.byCreatedAtThenId())
                .map(skillMapper::toDTO)
                .toList();

        return studentMapper.toCardDTO(studentEnt, skillDTOList);
    }
}
