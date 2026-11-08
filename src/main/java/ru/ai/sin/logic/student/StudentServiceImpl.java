package ru.ai.sin.logic.student;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.embeddables.ContactInformation;
import ru.ai.sin.models.embeddables.UserInformation;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.exception.models.ProfileIncompleteException;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.FileHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.account.StudentReviewState;
import ru.ai.sin.logic.chat.ChatRepo;
import ru.ai.sin.logic.experience.ExperienceRepo;
import ru.ai.sin.logic.institution.InstitutionRepo;
import ru.ai.sin.logic.portfolio.PortfolioRepo;
import ru.ai.sin.logic.request.RequestRepo;
import ru.ai.sin.logic.student.dto.*;
import ru.ai.sin.logic.vacancy.VacancyApplicationRepo;
import ru.ai.sin.logic.profile.ProfileCommunicationGateService;
import ru.ai.sin.logic.profile.dto.CommunicationReadinessRes;
import ru.ai.sin.logic.registration.RegistrationPasswordPolicy;
import ru.ai.sin.logic.verification.VerificationOtpMailer;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.logic.speciality.SpecialityEnt;
import ru.ai.sin.tools.SpecialityTools;
import ru.ai.sin.tools.StudentTools;
import ru.ai.sin.tools.UserTools;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;
import ru.ai.sin.models.enums.JobSearchStatus;
import ru.ai.sin.models.enums.RoleEnum;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class StudentServiceImpl implements StudentService {

    private final StudentRepo studentRepo;
    private final PortfolioRepo portfolioRepo;
    private final ExperienceRepo experienceRepo;
    private final InstitutionRepo institutionRepo;
    private final RequestRepo requestRepo;
    private final ChatRepo chatRepo;
    private final VacancyApplicationRepo vacancyApplicationRepo;

    private final StudentMapper studentMapper;

    private final StudentTools studentTools;
    private final SpecialityTools specialityTools;
    private final StudentSkillsMutator studentSkillsMutator;

    private final FileHelper fileHelper;
    private final SecurityHelper securityHelper;
    private final AccountAccessHelper accountAccessHelper;
    private final UserTools userTools;
    private final UserRepo userRepo;
    private final PasswordEncoder passwordEncoder;
    private final RegistrationPasswordPolicy registrationPasswordPolicy;
    private final ProfileCommunicationGateService profileCommunicationGateService;
    private final VerificationOtpMailer verificationOtpMailer;
    private final StudentReviewState studentReviewState;

    /**
     * Один read-only boundary на загрузку + маппинг: {@link StudentTools#getStudentOrThrow} завершает свой вложенный tx,
     * без внешней транзакции {@link StudentTools#mapToDTO} обратился бы к LAZY вне сессии.
     */
    @Override
    @Transactional(readOnly = true)
    public StudentDTO getById(UUID id) {
        accountAccessHelper.requireCanReadStudentResumeDetails(id);
        return studentTools.mapToDTO(studentTools.getStudentOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StudentDTO> getLinkedForCurrentUser() {
        return userTools.findCurrentUserFetchingLinks()
                .filter(u -> u.getRole() == RoleEnum.STUDENT && u.getStudent() != null)
                .map(u -> studentTools.mapToDTO(u.getStudent()));
    }

    @Override
    @Transactional
    public void setPhoto(UUID id, MultipartFile file) {
        accountAccessHelper.requireStudentOwnsProfile(id);
        fileHelper.validateMultipart(file);

        StudentEnt studentEnt = studentTools.getStudentOrThrow(id);

        String filePath = fileHelper.saveFile(file, studentEnt.getId().toString());

        if (filePath == null) {
            throw new BadRequestException("Failed to save file");
        }

        studentEnt.setImagePath(filePath);
        StudentProfileScoring.applyTo(studentEnt);
        studentReviewState.onStudentSelfEdit(studentEnt);
    }

    @Override
    @Transactional
    public void deletePhoto(UUID id) {
        accountAccessHelper.requireStudentOwnsProfile(id);
        StudentEnt studentEnt = studentTools.getStudentOrThrow(id);
        String imagePath = studentEnt.getImagePath();
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }
        try {
            fileHelper.deleteFile(imagePath);
        } catch (FileNotFoundException ignored) {
            // файла уже нет, путь в анкете всё равно очищаем
        }
        studentEnt.setImagePath(null);
        StudentProfileScoring.applyTo(studentEnt);
        studentReviewState.onStudentSelfEdit(studentEnt);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentCardDTO> getAllCardsByFilter(
            Pageable pageable,
            FilterStudentReq filterStudentReq
    ) {
        UserEnt currentUser = accountAccessHelper.requireCurrentUser();
        Page<StudentEnt> page = loadCatalogPage(pageable, filterStudentReq, currentUser);

        return new PageResponse<>(
                page.getContent().stream().map(studentTools::mapToCardDTO).toList(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    /**
     * Полный DTO после выборки: маппер читает LAZY-поля (например {@code bio}), поэтому метод в read-only транзакции
     * (иначе {@code LazyInitializationException} после {@code findAll}).
     */
    @Override
    @Transactional(readOnly = true)
    public PageResponse<StudentDTO> getAllByFilter(
            Pageable pageable,
            FilterStudentReq filterStudentReq
    ) {
        UserEnt currentUser = accountAccessHelper.requireCurrentUser();
        Page<StudentEnt> page = loadCatalogPage(pageable, filterStudentReq, currentUser);

        return new PageResponse<>(
                page.getContent().stream().map(studentTools::mapToDTO).toList(),
                pageable.getPageNumber(),
                pageable.getPageSize(),
                page.getTotalElements(),
                page.getTotalPages());
    }

    private Page<StudentEnt> loadCatalogPage(
            Pageable pageable,
            FilterStudentReq filterStudentReq,
            UserEnt currentUser
    ) {
        boolean admin = currentUser.getRole() == RoleEnum.ADMIN;
        UUID ownId = null;
        if (!admin && currentUser.getRole() == RoleEnum.STUDENT
                && currentUser.getAccountStatus() == AccountStatus.APPROVED
                && currentUser.getStudent() != null) {
            ownId = currentUser.getStudent().getId();
        }
        Sort sort = StudentSortResolver.resolve(filterStudentReq);
        Pageable effectivePageable = org.springframework.data.domain.PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sort);
        return StudentSpecifications.findPage(
                studentRepo, effectivePageable, filterStudentReq, admin, false, !admin, ownId);
    }

    @Override
    @Transactional
    public StudentDTO create(AddStudentReq addStudentReq) {
        StudentEnt studentEnt = studentMapper.toEntity(addStudentReq);

        SpecialityEnt specialityEnt = specialityTools.getSpecialityOrThrow(addStudentReq.specialityId());

        studentEnt.setSpeciality(specialityEnt);
        studentSkillsMutator.replaceSkills(studentEnt, addStudentReq.skillsIds());
        applyCreateCatalogFlags(addStudentReq, studentEnt);
        studentEnt = saveNewStudent(studentEnt, addStudentReq.email(), addStudentReq.telegramUsername());

        StudentProfileScoring.applyTo(studentEnt);
        studentRepo.save(studentEnt);

        linkStudentAccount(studentEnt, addStudentReq.username(), addStudentReq.password(), buildStudentDisplayName(addStudentReq));

        StudentDTO studentDTO = studentTools.mapToDTO(studentEnt);

        log.info("User: {}, created a new student: {}", securityHelper.getCurrentUsername(), studentDTO);

        return studentDTO;
    }

    @Override
    @Transactional
    public StudentDTO update(
            UUID id,
            UpdateStudentReq updateStudentReq
    ) {
        StudentEnt studentEnt = studentTools.getStudentOrThrow(id);

        SpecialityEnt specialityEnt = specialityTools.getSpecialityOrThrow(updateStudentReq.specialityId());

        studentMapper.updateEntityFromDto(updateStudentReq, studentEnt);

        if (studentEnt.getContactInformation() == null) {
            studentEnt.setContactInformation(new ContactInformation());
        }

        studentEnt.getUserInformation().setFirstName(updateStudentReq.firstName());
        studentEnt.getUserInformation().setLastName(updateStudentReq.lastName());
        studentEnt.setMiddleName(updateStudentReq.middleName());
        studentEnt.setGender(updateStudentReq.gender());
        studentEnt.setSpeciality(specialityEnt);
        studentSkillsMutator.replaceSkills(studentEnt, updateStudentReq.skillsIds());

        if (updateStudentReq.publicProfileConsent() != null) {
            studentEnt.setPublicProfileConsent(updateStudentReq.publicProfileConsent());
        }
        if (Boolean.TRUE.equals(updateStudentReq.clearManualSortOrder())) {
            studentEnt.setManualSortOrder(null);
        } else if (updateStudentReq.manualSortOrder() != null) {
            studentEnt.setManualSortOrder(updateStudentReq.manualSortOrder());
        }
        StudentProfileScoring.applyTo(studentEnt);

        StudentDTO studentDTO = studentTools.mapToDTO(studentEnt);

        log.info("User: {}, updated a student: {} with data: {}", securityHelper.getCurrentUsername(), id, studentDTO);

        return studentDTO;
    }

    @Override
    @Transactional
    public StudentDTO patch(UUID id, PatchStudentReq patchStudentReq) {
        StudentEnt studentEnt = studentTools.getStudentOrThrow(id);

        applyOptionalResumeFields(
                studentEnt,
                patchStudentReq.city(),
                patchStudentReq.hhLink(),
                patchStudentReq.birthDate(),
                patchStudentReq.bio(),
                patchStudentReq.course(),
                patchStudentReq.busyness(),
                patchStudentReq.desiredBusyness(),
                patchStudentReq.jobSearchStatus(),
                patchStudentReq.firstName(),
                patchStudentReq.lastName(),
                patchStudentReq.middleName(),
                patchStudentReq.gender(),
                patchStudentReq.email(),
                patchStudentReq.phoneNumber(),
                patchStudentReq.telegramUsername(),
                patchStudentReq.specialityId(),
                patchStudentReq.skillsIds(),
                patchStudentReq.publicProfileConsent());
        if (patchStudentReq.catalogVisible() != null) {
            studentEnt.setCatalogVisible(
                    studentReviewState.catalogVisible(studentEnt, patchStudentReq.catalogVisible()));
        }
        if (Boolean.TRUE.equals(patchStudentReq.clearManualSortOrder())) {
            studentEnt.setManualSortOrder(null);
        } else if (patchStudentReq.manualSortOrder() != null) {
            studentEnt.setManualSortOrder(patchStudentReq.manualSortOrder());
        }

        StudentProfileScoring.applyTo(studentEnt);

        StudentDTO studentDTO = studentTools.mapToDTO(studentEnt);
        log.info("User: {}, patched student: {} with data: {}", securityHelper.getCurrentUsername(), id, studentDTO);
        return studentDTO;
    }

    @Override
    @Transactional
    public StudentDTO patchMe(PatchStudentMeReq req) {
        UserEnt user = userRepo.findByUsernameFetchingLinks(securityHelper.getCurrentUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != RoleEnum.STUDENT || user.getStudent() == null) {
            throw new BadRequestException("К аккаунту не привязана карточка студента");
        }
        StudentEnt studentEnt = user.getStudent();
        accountAccessHelper.requireStudentCanMutateResume(studentEnt.getId());

        if (req.hintsDisabled() != null) {
            user.setHintsDisabled(req.hintsDisabled());
            userRepo.save(user);
        }

        applyOptionalResumeFields(
                studentEnt,
                req.city(),
                req.hhLink(),
                req.birthDate(),
                req.bio(),
                req.course(),
                req.busyness(),
                req.desiredBusyness(),
                req.jobSearchStatus(),
                req.firstName(),
                req.lastName(),
                req.middleName(),
                req.gender(),
                req.email(),
                req.phoneNumber(),
                req.telegramUsername(),
                req.specialityId(),
                req.skillsIds(),
                req.publicProfileConsent());

        StudentProfileScoring.applyTo(studentEnt);
        studentReviewState.onStudentSelfEdit(studentEnt);
        return studentTools.mapToDTO(studentEnt);
    }

    @Override
    @Transactional(readOnly = true)
    public void completeRegistration() {
        UserEnt user = currentStudentPendingEmailVerified();
        verificationOtpMailer.trySendRegistrationSuccess(studentEmail(user), user.getUsername());
    }

    @Override
    @Transactional
    public void submitForReview() {
        UserEnt user = currentStudentPendingEmailVerified();
        CommunicationReadinessRes readiness = profileCommunicationGateService.evaluateForCurrentUser();
        if (!readiness.ready()) {
            throw new ProfileIncompleteException(readiness.missingFields());
        }
        verificationOtpMailer.trySendProfileSubmitted(studentEmail(user));
    }

    private UserEnt currentStudentPendingEmailVerified() {
        UserEnt user = userRepo.findByUsernameFetchingLinks(securityHelper.getCurrentUsername())
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.getRole() != RoleEnum.STUDENT || user.getStudent() == null) {
            throw new BadRequestException("К аккаунту не привязана карточка студента");
        }
        if (!user.isEmailVerified()) {
            throw new BadRequestException("Сначала подтвердите почту");
        }
        if (user.getAccountStatus() != AccountStatus.PENDING_APPROVAL) {
            throw new BadRequestException("Аккаунт уже обработан");
        }
        return user;
    }

    private static String studentEmail(UserEnt user) {
        if (user.getRegistrationEmail() != null && !user.getRegistrationEmail().isBlank()) {
            return user.getRegistrationEmail().trim();
        }
        if (user.getStudent().getUserInformation() != null
                && user.getStudent().getUserInformation().getEmail() != null
                && !user.getStudent().getUserInformation().getEmail().isBlank()) {
            return user.getStudent().getUserInformation().getEmail().trim();
        }
        throw new BadRequestException("У аккаунта не указана почта");
    }

    @Override
    @Transactional
    public void deleteById(UUID id) {
        StudentEnt studentEnt = studentTools.getStudentOrThrow(id);
        StudentDTO snapshot = studentTools.mapToDTO(studentEnt);

        try {
            vacancyApplicationRepo.deleteByStudentId(id);
            requestRepo.deleteByStudent_Id(id);
            chatRepo.deleteByStudent_Id(id);
            experienceRepo.deleteByStudent_Id(id);
            institutionRepo.deleteByStudent_Id(id);
            portfolioRepo.deleteByStudent_Id(id);
            userRepo.findByStudent_Id(id).ifPresent(userRepo::delete);
            studentRepo.delete(studentEnt);
        }
        catch (DataIntegrityViolationException ex) {
            log.warn("Error while deleting student: {}", ex.getMessage());

            throw new BadRequestException("Error while deleting student");
        }

        log.info("User: {}, deleted a student: {} with data: {}", securityHelper.getCurrentUsername(), id, snapshot);
    }

    private void linkStudentAccount(StudentEnt studentEnt, String username, String password, String displayName) {
        if (username == null || username.isBlank()) {
            throw new BadRequestException("Укажите логин для учётной записи");
        }
        if (password == null || password.isBlank()) {
            throw new BadRequestException("Укажите пароль для учётной записи");
        }
        String login = username.trim();
        registrationPasswordPolicy.validate(password);
        if (userRepo.existsByUsername(login)) {
            throw new BadRequestException("Пользователь с таким логином уже существует: " + login);
        }
        if (userRepo.findByStudent_Id(studentEnt.getId()).isPresent()) {
            throw new BadRequestException("К этой карточке студента уже привязан пользователь");
        }
        UserEnt user = new UserEnt(
                RoleEnum.STUDENT,
                displayName,
                login,
                passwordEncoder.encode(password)
        );
        user.setStudent(studentEnt);
        user.setEmailVerified(true);
        try {
            userRepo.save(user);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Failed to link student account: {}", ex.getMessage());
            throw new BadRequestException("Не удалось создать учётную запись для студента");
        }
    }

    private static String buildStudentDisplayName(AddStudentReq req) {
        String first = req.firstName() != null ? req.firstName().trim() : "";
        String last = req.lastName() != null ? req.lastName().trim() : "";
        String middle = req.middleName() != null ? req.middleName().trim() : "";
        String combined = (last + " " + first + " " + middle).trim();
        return combined.isEmpty() ? null : combined;
    }

    private StudentEnt saveNewStudent(StudentEnt studentEnt, String email, String telegramUsername) {
        try {
            return studentRepo.save(studentEnt);
        } catch (DataIntegrityViolationException ex) {
            log.warn("Student already exists: {}, {}", email, telegramUsername);
            throw new BadRequestException("Student already exists: %s, %s".formatted(email, telegramUsername));
        }
    }

    private void applyOptionalResumeFields(
            StudentEnt studentEnt,
            String city,
            String hhLink,
            LocalDate birthDate,
            String bio,
            CourseEnum course,
            BusynessEnum busyness,
            BusynessEnum desiredBusyness,
            JobSearchStatus jobSearchStatus,
            String firstName,
            String lastName,
            String middleName,
            ru.ai.sin.models.enums.GenderEnum gender,
            String email,
            String phoneNumber,
            String telegramUsername,
            Long specialityId,
            List<Long> skillsIds,
            Boolean publicProfileConsent
    ) {
        if (city != null) {
            studentEnt.setCity(city);
        }
        if (hhLink != null) {
            studentEnt.setHhLink(hhLink);
        }
        if (birthDate != null) {
            studentEnt.setBirthDate(birthDate);
        }
        if (bio != null) {
            studentEnt.setBio(bio);
        }
        if (course != null) {
            studentEnt.setCourse(course);
        }
        if (busyness != null) {
            studentEnt.setBusyness(busyness);
        }
        if (desiredBusyness != null) {
            studentEnt.setDesiredBusyness(desiredBusyness);
        }
        if (jobSearchStatus != null) {
            studentEnt.setJobSearchStatus(jobSearchStatus);
        }

        if (studentEnt.getUserInformation() == null) {
            studentEnt.setUserInformation(new UserInformation());
        }
        if (studentEnt.getContactInformation() == null) {
            studentEnt.setContactInformation(new ContactInformation());
        }
        if (firstName != null) {
            studentEnt.getUserInformation().setFirstName(firstName);
        }
        if (lastName != null) {
            studentEnt.getUserInformation().setLastName(lastName);
        }
        if (middleName != null) {
            studentEnt.setMiddleName(middleName);
        }
        if (gender != null) {
            studentEnt.setGender(gender);
        }
        if (email != null) {
            studentEnt.getUserInformation().setEmail(email);
        }
        if (phoneNumber != null) {
            studentEnt.getContactInformation().setPhoneNumber(phoneNumber);
        }
        if (telegramUsername != null) {
            studentEnt.getContactInformation().setTelegramUsername(telegramUsername);
        }

        if (specialityId != null) {
            studentEnt.setSpeciality(specialityTools.getSpecialityOrThrow(specialityId));
        }
        if (skillsIds != null) {
            studentSkillsMutator.replaceSkills(studentEnt, skillsIds);
        }
        if (publicProfileConsent != null) {
            studentEnt.setPublicProfileConsent(publicProfileConsent);
        }
    }

    /**
     * Опции каталога при создании: согласие на витрину и ручной порядок (маппер по-прежнему их не мапит из AddStudentReq).
     */
    private static void applyCreateCatalogFlags(AddStudentReq addStudentReq, StudentEnt studentEnt) {
        studentEnt.setPublicProfileConsent(Boolean.TRUE.equals(addStudentReq.publicProfileConsent()));
        studentEnt.setManualSortOrder(addStudentReq.manualSortOrder());
    }

    @Override
    @Transactional
    public BulkStudentVisibilityResult bulkUpdateVisibility(BulkStudentVisibilityReq req) {
        List<StudentEnt> students = resolveBulkVisibilityTargets(req);
        int updated = 0;
        for (StudentEnt student : students) {
            boolean changed = false;
            if (req.catalogVisible() != null) {
                boolean visible = studentReviewState.catalogVisible(student, req.catalogVisible());
                if (student.isCatalogVisible() != visible) {
                    student.setCatalogVisible(visible);
                    changed = true;
                }
            }
            if (req.publicProfileConsent() != null
                    && student.isPublicProfileConsent() != req.publicProfileConsent()) {
                student.setPublicProfileConsent(req.publicProfileConsent());
                changed = true;
            }
            if (changed) {
                studentRepo.save(student);
                updated++;
            }
        }
        log.info(
                "Bulk visibility update by {}: matched={} updated={} catalogVisible={} publicProfileConsent={}",
                securityHelper.getCurrentUsername(),
                students.size(),
                updated,
                req.catalogVisible(),
                req.publicProfileConsent());
        return new BulkStudentVisibilityResult(updated, students.size());
    }

    private List<StudentEnt> resolveBulkVisibilityTargets(BulkStudentVisibilityReq req) {
        if (req.studentIds() != null && !req.studentIds().isEmpty()) {
            return studentRepo.findAllById(req.studentIds());
        }
        if (!req.effectiveAll()) {
            throw new BadRequestException("Укажите studentIds или all=true");
        }
        if (req.effectiveOnlyApproved()) {
            return studentRepo.findAllWithApprovedAccount();
        }
        return studentRepo.findAll();
    }

    @Override
    @Transactional
    public void reorder(ReorderStudentsReq req) {
        List<UUID> ids = req.orderedIds();
        Set<UUID> unique = new HashSet<>(ids);
        if (unique.size() != ids.size()) {
            throw new BadRequestException("Duplicate ids in reorder list");
        }
        for (int i = 0; i < ids.size(); i++) {
            UUID id = ids.get(i);
            StudentEnt student = studentTools.getStudentOrThrow(id);
            student.setManualSortOrder(i);
            studentRepo.save(student);
        }
    }
}
