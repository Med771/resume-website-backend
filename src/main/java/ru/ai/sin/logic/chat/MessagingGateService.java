package ru.ai.sin.logic.chat;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.logic.request.RequestRepo;
import ru.ai.sin.logic.vacancy.VacancyApplicationRepo;
import ru.ai.sin.models.enums.ResultEnum;
import ru.ai.sin.models.enums.VacancyApplicationStatus;

import java.util.List;
import java.util.UUID;

/**
 * Единая проверка: разрешена ли переписка между рекрутёром и студентом.
 */
@Service
@RequiredArgsConstructor
public class MessagingGateService {

    private static final List<ResultEnum> REQUEST_MESSAGING_ALLOWED = List.of(
            ResultEnum.STUDENT_CONFIRMED,
            ResultEnum.SUCCESS,
            ResultEnum.RECRUITER_CONFIRMED
    );

    private final RequestRepo requestRepo;
    private final VacancyApplicationRepo vacancyApplicationRepo;

    @Transactional(readOnly = true)
    public boolean isMessagingAllowed(UUID recruiterId, UUID studentId) {
        if (requestRepo.existsByRecruiter_IdAndStudent_IdAndResultIn(
                recruiterId, studentId, REQUEST_MESSAGING_ALLOWED)) {
            return true;
        }
        return vacancyApplicationRepo.existsByRecruiterAndStudentAndStatus(
                recruiterId, studentId, VacancyApplicationStatus.ACCEPTED);
    }
}
