package ru.ai.sin.logic.vacancy;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.logic.chat.ChatEnt;
import ru.ai.sin.logic.chat.ChatService;
import ru.ai.sin.logic.chat.ChatSystemEvent;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.vacancy.dto.ApplyVacancyReq;
import ru.ai.sin.logic.vacancy.dto.RejectApplicationReq;
import ru.ai.sin.logic.vacancy.dto.VacancyApplicationDTO;
import ru.ai.sin.tools.StudentTools;
import ru.ai.sin.tools.VacancyApplicationTools;
import ru.ai.sin.tools.UserTools;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.enums.RoleEnum;
import ru.ai.sin.models.enums.VacancyApplicationStatus;
import ru.ai.sin.models.enums.VacancyStatus;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VacancyApplicationServiceImpl implements VacancyApplicationService {

    private final VacancyApplicationRepo vacancyApplicationRepo;
    private final VacancyRepo vacancyRepo;
    private final UserTools userTools;
    private final StudentTools studentTools;
    private final VacancyApplicationTools vacancyApplicationTools;
    private final ChatService chatService;
    private final AccountAccessHelper accountAccessHelper;

    @Override
    @Transactional
    public VacancyApplicationDTO apply(UUID vacancyId, ApplyVacancyReq req) {
        accountAccessHelper.requireApprovedAccount();
        StudentEnt student = requireCurrentStudent();
        if (!student.isCatalogVisible()) {
            throw new BadRequestException("Отклик недоступен: профиль студента ещё не опубликован в каталоге");
        }
        VacancyEnt vacancy = vacancyRepo.findWithDetailsById(vacancyId)
                .orElseThrow(() -> new NotFoundException("Вакансия не найдена: " + vacancyId));
        assertVacancyAcceptsApplications(vacancy);
        if (vacancyApplicationRepo.findByVacancy_IdAndStudent_Id(vacancyId, student.getId()).isPresent()) {
            throw new BadRequestException("Вы уже откликались на эту вакансию");
        }
        VacancyApplicationEnt app = new VacancyApplicationEnt();
        app.setVacancy(vacancy);
        app.setStudent(student);
        app.setStatus(VacancyApplicationStatus.SUBMITTED);
        app.setCoverLetter(req != null ? req.coverLetter() : null);
        try {
            app = vacancyApplicationRepo.save(app);
        } catch (DataIntegrityViolationException ex) {
            throw new BadRequestException("Вы уже откликались на эту вакансию");
        }
        log.info("Vacancy application submitted: vacancyId={} studentId={}", vacancyId, student.getId());
        return toDto(app);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VacancyApplicationDTO> listMine(Pageable pageable) {
        StudentEnt student = requireCurrentStudent();
        Page<VacancyApplicationEnt> page = vacancyApplicationRepo.findByStudent_IdOrderByTimestamps_CreatedAtDesc(
                student.getId(), pageable);
        return new PageResponse<>(
                page.getContent().stream().map(this::toDto).toList(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional
    public void withdraw(UUID applicationId) {
        VacancyApplicationEnt app = loadOwnedByStudent(applicationId);
        if (app.getStatus() != VacancyApplicationStatus.SUBMITTED) {
            throw new BadRequestException("Отозвать можно только отправленный отклик");
        }
        app.setStatus(VacancyApplicationStatus.WITHDRAWN);
        vacancyApplicationRepo.save(app);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<VacancyApplicationDTO> listForVacancy(UUID vacancyId, Pageable pageable) {
        VacancyEnt vacancy = vacancyRepo.findWithDetailsById(vacancyId)
                .orElseThrow(() -> new NotFoundException("Вакансия не найдена: " + vacancyId));
        assertVacancyOwner(vacancy);
        Page<VacancyApplicationEnt> page = vacancyApplicationRepo.findByVacancy_IdOrderByTimestamps_CreatedAtDesc(
                vacancyId, pageable);
        return new PageResponse<>(
                page.getContent().stream().map(this::toDto).toList(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    @Override
    @Transactional
    public VacancyApplicationDTO accept(UUID vacancyId, UUID applicationId) {
        VacancyApplicationEnt app = loadForRecruiterAction(vacancyId, applicationId);
        if (app.getStatus() != VacancyApplicationStatus.SUBMITTED) {
            throw new BadRequestException("Принять можно только отправленный отклик");
        }
        RecruiterEnt recruiter = app.getVacancy().getRecruiter();
        StudentEnt student = app.getStudent();
        ChatEnt chat = chatService.getOrCreateChat(recruiter, student);
        app.setAppChat(chat);
        app.setStatus(VacancyApplicationStatus.ACCEPTED);
        vacancyApplicationRepo.save(app);
        chatService.postSystemMessage(
                chat,
                ChatSystemEvent.VACANCY_APPLICATION_ACCEPTED,
                "Отклик на вакансию «" + app.getVacancy().getTitle() + "» принят. Можно начинать переписку."
        );
        log.info("Vacancy application accepted: id={} chatId={}", applicationId, chat.getId());
        return toDto(app);
    }

    @Override
    @Transactional
    public VacancyApplicationDTO reject(UUID vacancyId, UUID applicationId, RejectApplicationReq req) {
        VacancyApplicationEnt app = loadForRecruiterAction(vacancyId, applicationId);
        if (app.getStatus() != VacancyApplicationStatus.SUBMITTED) {
            throw new BadRequestException("Отклонить можно только отправленный отклик");
        }
        String reason = req != null && req.rejectionReason() != null && !req.rejectionReason().isBlank()
                ? req.rejectionReason().trim()
                : null;
        app.setRejectionReason(reason);
        app.setStatus(VacancyApplicationStatus.REJECTED);
        vacancyApplicationRepo.save(app);
        return toDto(app);
    }

    private VacancyApplicationEnt loadForRecruiterAction(UUID vacancyId, UUID applicationId) {
        VacancyApplicationEnt app = vacancyApplicationRepo.findWithDetailsById(applicationId)
                .orElseThrow(() -> new NotFoundException("Отклик не найден: " + applicationId));
        if (!app.getVacancy().getId().equals(vacancyId)) {
            throw new NotFoundException("Отклик не найден: " + applicationId);
        }
        assertVacancyOwner(app.getVacancy());
        return app;
    }

    private VacancyApplicationEnt loadOwnedByStudent(UUID applicationId) {
        VacancyApplicationEnt app = vacancyApplicationRepo.findWithDetailsById(applicationId)
                .orElseThrow(() -> new NotFoundException("Отклик не найден: " + applicationId));
        StudentEnt student = requireCurrentStudent();
        if (!app.getStudent().getId().equals(student.getId())) {
            throw new AccessDeniedException("Это не ваш отклик");
        }
        return app;
    }

    private void assertVacancyOwner(VacancyEnt vacancy) {
        RecruiterEnt recruiter = userTools.findCurrentUserFetchingRecruiter()
                .map(UserEnt::getRecruiter)
                .orElse(null);
        if (recruiter == null || !recruiter.getId().equals(vacancy.getRecruiter().getId())) {
            throw new AccessDeniedException("Нет доступа к откликам этой вакансии");
        }
    }

    private StudentEnt requireCurrentStudent() {
        UserEnt user = userTools.findCurrentUserFetchingLinks()
                .orElseThrow(() -> new AccessDeniedException("Требуется авторизация"));
        if (user.getRole() != RoleEnum.STUDENT || user.getStudent() == null) {
            throw new AccessDeniedException("Только студент может выполнить это действие");
        }
        return user.getStudent();
    }

    private void assertVacancyAcceptsApplications(VacancyEnt vacancy) {
        if (vacancy.getStatus() != VacancyStatus.PUBLISHED) {
            throw new BadRequestException("На эту вакансию нельзя откликнуться");
        }
        if (!VacancyServiceImpl.isInPublicationWindow(vacancy)) {
            throw new BadRequestException("Вакансия вне окна публикации");
        }
        if (vacancy.getStatus() == VacancyStatus.CLOSED || vacancy.getStatus() == VacancyStatus.ARCHIVED) {
            throw new BadRequestException("Отклики на эту вакансию закрыты");
        }
    }

    private VacancyApplicationDTO toDto(VacancyApplicationEnt app) {
        return vacancyApplicationTools.mapToDTO(app);
    }
}
