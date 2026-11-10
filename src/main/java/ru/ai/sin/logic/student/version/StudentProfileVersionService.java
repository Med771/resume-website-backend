package ru.ai.sin.logic.student.version;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.company.CompanyEnt;
import ru.ai.sin.logic.company.CompanyRepo;
import ru.ai.sin.logic.education.EducationEnt;
import ru.ai.sin.logic.education.EducationRepo;
import ru.ai.sin.logic.experience.ExperienceEnt;
import ru.ai.sin.logic.experience.ExperienceRepo;
import ru.ai.sin.logic.experience.dto.AddExperienceReq;
import ru.ai.sin.logic.experience.dto.ExperienceDTO;
import ru.ai.sin.logic.experience.dto.ExperienceRes;
import ru.ai.sin.logic.experience.dto.FilterExperienceReq;
import ru.ai.sin.logic.experience.dto.UpdateExperienceReq;
import ru.ai.sin.logic.institution.InstitutionEnt;
import ru.ai.sin.logic.institution.InstitutionRepo;
import ru.ai.sin.logic.institution.dto.AddInstitutionReq;
import ru.ai.sin.logic.institution.dto.FilterInstitutionReq;
import ru.ai.sin.logic.institution.dto.InstitutionDTO;
import ru.ai.sin.logic.institution.dto.InstitutionRes;
import ru.ai.sin.logic.institution.dto.UpdateInstitutionReq;
import ru.ai.sin.logic.portfolio.PortfolioEnt;
import ru.ai.sin.logic.portfolio.PortfolioRepo;
import ru.ai.sin.logic.portfolio.dto.AddPortfolioReq;
import ru.ai.sin.logic.portfolio.dto.FilterPortfolioReq;
import ru.ai.sin.logic.portfolio.dto.PortfolioDTO;
import ru.ai.sin.logic.skill.SkillEnt;
import ru.ai.sin.logic.skill.SkillRepo;
import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.logic.speciality.SpecialityEnt;
import ru.ai.sin.logic.speciality.SpecialityRepo;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentProfileScoring;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.student.dto.PatchStudentMeReq;
import ru.ai.sin.logic.student.dto.PatchStudentReq;
import ru.ai.sin.logic.student.dto.StudentDTO;
import ru.ai.sin.logic.student.dto.StudentProfileVersionItemDTO;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.models.embeddables.ContactInformation;
import ru.ai.sin.models.embeddables.UserInformation;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.EducationType;
import ru.ai.sin.models.enums.ExperienceType;
import ru.ai.sin.models.enums.RoleEnum;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class StudentProfileVersionService {

    static final Duration GRACE = Duration.ofMinutes(5);

    private final StudentProfileVersionRepo versionRepo;
    private final StudentRepo studentRepo;
    private final UserRepo userRepo;
    private final ExperienceRepo experienceRepo;
    private final InstitutionRepo institutionRepo;
    private final PortfolioRepo portfolioRepo;
    private final SkillRepo skillRepo;
    private final SpecialityRepo specialityRepo;
    private final CompanyRepo companyRepo;
    private final EducationRepo educationRepo;
    private final SecurityHelper securityHelper;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional(readOnly = true)
    public boolean hasPublishedVersion(UUID studentId) {
        return studentId != null && versionRepo.existsByStudentIdAndStatus(studentId, StudentProfileVersionStatus.APPROVED);
    }

    @Transactional
    public void expireDueDrafts() {
        LocalDateTime now = now();
        for (StudentProfileVersionEnt row : versionRepo.findByStatusAndGraceEndsAtBefore(StudentProfileVersionStatus.DRAFT, now)) {
            markPendingIfApproved(row.getStudentId());
        }
    }

    @Transactional(readOnly = true)
    public LocalDateTime activeGraceEndsAt(UUID studentId) {
        if (studentId == null) {
            return null;
        }
        return versionRepo.findFirstByStudentIdOrderByVersionNumberDesc(studentId)
                .filter(row -> row.getStatus() == StudentProfileVersionStatus.DRAFT)
                .map(StudentProfileVersionEnt::getGraceEndsAt)
                .filter(ends -> ends != null && ends.isAfter(now()))
                .orElse(null);
    }

    @Transactional
    public Optional<StudentDTO> draftCard(StudentEnt live) {
        if (live == null || live.getId() == null || !viewerSeesDraft(live.getId())) {
            return Optional.empty();
        }
        return unpublished(live.getId()).map(snap -> toCard(snap, live));
    }

    @Transactional(readOnly = true)
    public StudentDTO publishedCard(StudentEnt live) {
        if (live == null || !hasPublishedVersion(live.getId())) {
            throw new NotFoundException("Одобренной анкеты ещё нет");
        }
        return toCard(read(latestApproved(live.getId())), live);
    }

    @Transactional(readOnly = true)
    public List<StudentProfileVersionItemDTO> listOwn(UUID studentId) {
        requireOwn(studentId);
        return versionRepo.findByStudentIdOrderByVersionNumberAsc(studentId).stream()
                .map(row -> new StudentProfileVersionItemDTO(
                        row.getVersionNumber(),
                        row.getStatus().name(),
                        row.getCreatedAt()))
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentDTO ownVersion(StudentEnt live, int number) {
        requireOwn(live.getId());
        StudentProfileVersionEnt row = versionRepo.findByStudentIdAndVersionNumber(live.getId(), number)
                .orElseThrow(() -> new NotFoundException("Версия не найдена"));
        return toCard(read(row), live);
    }

    @Transactional(readOnly = true)
    public StudentDTO publishedForCurrent() {
        return publishedCard(currentLive());
    }

    @Transactional(readOnly = true)
    public List<StudentProfileVersionItemDTO> listForCurrent() {
        StudentEnt live = currentLive();
        requireOwn(live.getId());
        return listOwn(live.getId());
    }

    @Transactional(readOnly = true)
    public StudentDTO versionForCurrent(int number) {
        return ownVersion(currentLive(), number);
    }

    @Transactional(readOnly = true)
    public List<StudentProfileVersionItemDTO> listForAdmin(UUID studentId) {
        return versionRepo.findByStudentIdOrderByVersionNumberAsc(studentId).stream()
                .map(row -> new StudentProfileVersionItemDTO(
                        row.getVersionNumber(), row.getStatus().name(), row.getCreatedAt()))
                .toList();
    }

    @Transactional
    public void revertDraft(UUID studentId) {
        requireOwn(studentId);
        StudentProfileVersionEnt latest = versionRepo.findFirstByStudentIdOrderByVersionNumberDesc(studentId)
                .orElseThrow(() -> new BadRequestException("Черновика нет"));
        if (latest.getStatus() != StudentProfileVersionStatus.DRAFT) {
            throw new BadRequestException("Черновика нет");
        }
        if (latest.getGraceEndsAt() == null || !latest.getGraceEndsAt().isAfter(now())) {
            markPendingIfApproved(studentId);
            return;
        }
        versionRepo.deleteByStudentIdAndStatus(studentId, StudentProfileVersionStatus.DRAFT);
    }

    @Transactional
    public boolean captureCardPatch(UUID studentId, PatchStudentMeReq req) {
        if (!isStudent() || !hasPublishedVersion(studentId)) {
            return false;
        }
        edit(studentId, snap -> applyCard(snap, req), true);
        return true;
    }

    @Transactional
    public boolean captureAdminCard(UUID studentId, PatchStudentReq req) {
        if (!isAdmin() || !latestIsDraft(studentId)) {
            return false;
        }
        edit(studentId, snap -> applyAdminCard(snap, req), false);
        return true;
    }

    @Transactional
    public boolean capturePhoto(UUID studentId, String path) {
        if (!isStudent() || !hasPublishedVersion(studentId)) {
            return false;
        }
        String published = publishedImage(studentId);
        edit(studentId, snap -> {
            String previous = snap.getImagePath();
            snap.setImagePath(path);
            deleteDraftFile(previous, published, path);
        }, true);
        return true;
    }

    @Transactional
    public boolean capturePhotoDelete(UUID studentId) {
        if (!isStudent() || !hasPublishedVersion(studentId)) {
            return false;
        }
        String published = publishedImage(studentId);
        edit(studentId, snap -> {
            String previous = snap.getImagePath();
            snap.setImagePath(null);
            deleteDraftFile(previous, published, null);
        }, true);
        return true;
    }

    @Transactional
    public void rememberLiveApproved(UUID studentId) {
        if (!hasPublishedVersion(studentId) || latestIsDraft(studentId)) {
            return;
        }
        StudentEnt live = studentRepo.findById(studentId).orElse(null);
        if (live == null) {
            return;
        }
        saveNext(studentId, capture(live), null, StudentProfileVersionStatus.APPROVED);
    }

    @Transactional
    public void publishOnApprove(StudentEnt student) {
        if (student == null || student.getId() == null) {
            return;
        }
        Optional<StudentProfileVersionEnt> latest = versionRepo.findFirstByStudentIdOrderByVersionNumberDesc(student.getId());
        if (latest.isEmpty() || latest.get().getStatus() == StudentProfileVersionStatus.APPROVED) {
            saveNext(student.getId(), capture(student), null, StudentProfileVersionStatus.APPROVED);
            return;
        }
        copyOnto(student, read(latest.get()));
        latest.get().setStatus(StudentProfileVersionStatus.APPROVED);
        latest.get().setGraceEndsAt(null);
        versionRepo.save(latest.get());
    }

    @Transactional(readOnly = true)
    public void assertUnpublishedComplete(StudentEnt live) {
        Optional<StudentProfileVersionEnt> latest = versionRepo.findFirstByStudentIdOrderByVersionNumberDesc(live.getId());
        if (latest.isEmpty() || latest.get().getStatus() == StudentProfileVersionStatus.APPROVED) {
            return;
        }
        List<String> problems = problems(read(latest.get()));
        if (!problems.isEmpty()) {
            throw new BadRequestException("Анкета неполная: " + String.join(", ", problems));
        }
    }

    @Transactional(readOnly = true)
    public boolean hasUnpublished(UUID studentId) {
        return versionRepo.findFirstByStudentIdOrderByVersionNumberDesc(studentId)
                .filter(row -> row.getStatus() != StudentProfileVersionStatus.APPROVED)
                .isPresent();
    }

    @Transactional
    public void markLatestDraftRejected(UUID studentId) {
        versionRepo.findFirstByStudentIdOrderByVersionNumberDesc(studentId)
                .filter(row -> row.getStatus() == StudentProfileVersionStatus.DRAFT)
                .ifPresent(row -> {
                    row.setStatus(StudentProfileVersionStatus.REJECTED);
                    row.setGraceEndsAt(null);
                    versionRepo.save(row);
                });
    }

    @Transactional(readOnly = true)
    public Optional<PageResponse<ExperienceDTO>> draftExperiences(FilterExperienceReq filter, Pageable pageable) {
        if (filter == null || Boolean.TRUE.equals(filter.published()) || !seesDraftLists(filter.studentId())) {
            return Optional.empty();
        }
        List<ExperienceDTO> rows = unpublished(filter.studentId()).orElseThrow().getExperience().stream()
                .filter(item -> filter.companyId() == null || item.getCompanyId() == filter.companyId())
                .map(item -> new ExperienceDTO(
                        item.getCompanyId(),
                        item.getCompanyWebsite(),
                        filter.studentId(),
                        new ExperienceRes(item.getId(), item.getPosition(), item.getAdditionalInfo(),
                                item.getStartDate(), item.getEndDate(),
                                item.getExperienceType() == null ? ExperienceType.WORK : item.getExperienceType())))
                .toList();
        return Optional.of(page(rows, pageable));
    }

    @Transactional(readOnly = true)
    public Optional<PageResponse<InstitutionDTO>> draftEducation(FilterInstitutionReq filter, Pageable pageable) {
        if (filter == null || Boolean.TRUE.equals(filter.published()) || !seesDraftLists(filter.studentId())) {
            return Optional.empty();
        }
        List<InstitutionDTO> rows = unpublished(filter.studentId()).orElseThrow().getEducation().stream()
                .filter(item -> filter.educationId() == null || item.getEducationId() == filter.educationId())
                .map(item -> new InstitutionDTO(
                        item.getEducationId(),
                        filter.studentId(),
                        new InstitutionRes(item.getId(), item.getStartYear(), item.getEndYear(),
                                item.getEducationType() == null ? EducationType.STATE : item.getEducationType(),
                                item.getDirection(), item.getEducationLevel())))
                .toList();
        return Optional.of(page(rows, pageable));
    }

    @Transactional(readOnly = true)
    public Optional<PageResponse<PortfolioDTO>> draftPortfolio(FilterPortfolioReq filter, Pageable pageable) {
        if (filter == null || Boolean.TRUE.equals(filter.published()) || !seesDraftLists(filter.studentId())) {
            return Optional.empty();
        }
        List<PortfolioDTO> rows = unpublished(filter.studentId()).orElseThrow().getPortfolio().stream()
                .map(item -> new PortfolioDTO(item.getId(), item.getName(), item.getLink(), item.getAdditionalInfo(), filter.studentId()))
                .toList();
        return Optional.of(page(rows, pageable));
    }

    @Transactional
    public Optional<ExperienceDTO> captureExperienceCreate(UUID studentId, AddExperienceReq req) {
        if (!capturesSection(studentId)) {
            return Optional.empty();
        }
        ProfileSnapshot.ExperienceSnap[] created = new ProfileSnapshot.ExperienceSnap[1];
        edit(studentId, snap -> {
            ProfileSnapshot.ExperienceSnap item = new ProfileSnapshot.ExperienceSnap();
            item.setId(nextItemId(snap.getExperience().stream().map(ProfileSnapshot.ExperienceSnap::getId).toList()));
            item.setCompanyId(req.companyId());
            item.setCompanyWebsite(website(req.companyId()));
            item.setPosition(req.position());
            item.setAdditionalInfo(req.additionalInfo());
            item.setStartDate(req.startDate());
            item.setEndDate(req.endDate());
            item.setExperienceType(req.experienceType() == null ? ExperienceType.WORK : req.experienceType());
            snap.getExperience().add(item);
            created[0] = item;
        }, isStudent());
        return Optional.of(experienceDto(studentId, created[0]));
    }

    @Transactional
    public Optional<ExperienceDTO> captureExperienceUpdate(UUID studentId, long id, UpdateExperienceReq req) {
        if (!capturesSection(studentId)) {
            return Optional.empty();
        }
        edit(studentId, snap -> {
            ProfileSnapshot.ExperienceSnap item = snap.getExperience().stream()
                    .filter(row -> row.getId() == id)
                    .findFirst()
                    .orElseThrow(() -> new NotFoundException("Experience not found"));
            item.setCompanyId(req.companyId());
            item.setCompanyWebsite(website(req.companyId()));
            item.setPosition(req.position());
            item.setAdditionalInfo(req.additionalInfo());
            item.setStartDate(req.startDate());
            item.setEndDate(req.endDate());
            if (req.experienceType() != null) {
                item.setExperienceType(req.experienceType());
            }
        }, isStudent());
        return unpublished(studentId).flatMap(snap -> snap.getExperience().stream()
                .filter(row -> row.getId() == id).findFirst()).map(item -> experienceDto(studentId, item));
    }

    @Transactional
    public boolean captureExperienceDelete(UUID studentId, long id) {
        if (!capturesSection(studentId)) {
            return false;
        }
        edit(studentId, snap -> snap.getExperience().removeIf(row -> row.getId() == id), isStudent());
        return true;
    }

    @Transactional
    public Optional<InstitutionDTO> captureEducationCreate(UUID studentId, AddInstitutionReq req) {
        if (!capturesSection(studentId)) {
            return Optional.empty();
        }
        ProfileSnapshot.EducationSnap[] created = new ProfileSnapshot.EducationSnap[1];
        edit(studentId, snap -> {
            ProfileSnapshot.EducationSnap item = new ProfileSnapshot.EducationSnap();
            item.setId(nextItemId(snap.getEducation().stream().map(ProfileSnapshot.EducationSnap::getId).toList()));
            item.setEducationId(req.educationId());
            item.setStartYear(req.startYear());
            item.setEndYear(req.endYear());
            item.setEducationType(req.educationType() == null ? EducationType.STATE : req.educationType());
            item.setDirection(req.direction());
            item.setEducationLevel(req.educationLevel());
            snap.getEducation().add(item);
            created[0] = item;
        }, isStudent());
        return Optional.of(educationDto(studentId, created[0]));
    }

    @Transactional
    public Optional<InstitutionDTO> captureEducationUpdate(UUID studentId, long id, UpdateInstitutionReq req) {
        if (!capturesSection(studentId)) {
            return Optional.empty();
        }
        edit(studentId, snap -> {
            ProfileSnapshot.EducationSnap item = snap.getEducation().stream()
                    .filter(row -> row.getId() == id).findFirst()
                    .orElseThrow(() -> new NotFoundException("Institution not found"));
            item.setEducationId(req.educationId());
            item.setStartYear(req.startYear());
            item.setEndYear(req.endYear());
            if (req.educationType() != null) {
                item.setEducationType(req.educationType());
            }
            item.setDirection(req.direction());
            item.setEducationLevel(req.educationLevel());
        }, isStudent());
        return unpublished(studentId).flatMap(snap -> snap.getEducation().stream()
                .filter(row -> row.getId() == id).findFirst()).map(item -> educationDto(studentId, item));
    }

    @Transactional
    public boolean captureEducationDelete(UUID studentId, long id) {
        if (!capturesSection(studentId)) {
            return false;
        }
        edit(studentId, snap -> snap.getEducation().removeIf(row -> row.getId() == id), isStudent());
        return true;
    }

    @Transactional
    public Optional<PortfolioDTO> capturePortfolioCreate(UUID studentId, AddPortfolioReq req) {
        if (!capturesSection(studentId)) {
            return Optional.empty();
        }
        ProfileSnapshot.PortfolioSnap[] created = new ProfileSnapshot.PortfolioSnap[1];
        edit(studentId, snap -> {
            ProfileSnapshot.PortfolioSnap item = new ProfileSnapshot.PortfolioSnap();
            item.setId(nextItemId(snap.getPortfolio().stream().map(ProfileSnapshot.PortfolioSnap::getId).toList()));
            item.setName(req.name());
            item.setLink(req.link());
            item.setAdditionalInfo(req.additionalInfo());
            snap.getPortfolio().add(item);
            created[0] = item;
        }, isStudent());
        return Optional.of(new PortfolioDTO(created[0].getId(), created[0].getName(), created[0].getLink(), created[0].getAdditionalInfo(), studentId));
    }

    @Transactional
    public Optional<PortfolioDTO> capturePortfolioUpdate(UUID studentId, long id, AddPortfolioReq req) {
        if (!capturesSection(studentId)) {
            return Optional.empty();
        }
        edit(studentId, snap -> {
            ProfileSnapshot.PortfolioSnap item = snap.getPortfolio().stream()
                    .filter(row -> row.getId() == id).findFirst()
                    .orElseThrow(() -> new NotFoundException("Portfolio not found"));
            item.setName(req.name());
            item.setLink(req.link());
            item.setAdditionalInfo(req.additionalInfo());
        }, isStudent());
        return unpublished(studentId).flatMap(snap -> snap.getPortfolio().stream()
                .filter(row -> row.getId() == id).findFirst())
                .map(item -> new PortfolioDTO(item.getId(), item.getName(), item.getLink(), item.getAdditionalInfo(), studentId));
    }

    @Transactional
    public boolean capturePortfolioDelete(UUID studentId, long id) {
        if (!capturesSection(studentId)) {
            return false;
        }
        edit(studentId, snap -> snap.getPortfolio().removeIf(row -> row.getId() == id), isStudent());
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<UUID> findStudentIdByImage(String path) {
        if (path == null || path.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(versionRepo.findStudentIdBySnapshotImagePath(path));
    }

    private boolean capturesSection(UUID studentId) {
        if (isStudent()) {
            return hasPublishedVersion(studentId);
        }
        return isAdmin() && latestIsDraft(studentId);
    }

    private void edit(UUID studentId, Consumer<ProfileSnapshot> change, boolean studentEdit) {
        StudentProfileVersionEnt latest = versionRepo.findFirstByStudentIdOrderByVersionNumberDesc(studentId).orElse(null);
        ProfileSnapshot snap = latest == null ? new ProfileSnapshot() : read(latest);
        change.accept(snap);
        snap.setStudentId(studentId);
        LocalDateTime grace = graceFor(latest, studentEdit);
        saveNext(studentId, snap, grace, StudentProfileVersionStatus.DRAFT);
        if (studentEdit && latest != null && latest.getStatus() == StudentProfileVersionStatus.DRAFT
                && latest.getGraceEndsAt() != null && !latest.getGraceEndsAt().isAfter(now())) {
            markPendingIfApproved(studentId);
        }
    }

    private LocalDateTime graceFor(StudentProfileVersionEnt latest, boolean studentEdit) {
        if (!studentEdit) {
            return latest == null ? null : latest.getGraceEndsAt();
        }
        if (latest == null || latest.getStatus() != StudentProfileVersionStatus.DRAFT) {
            return now().plus(GRACE);
        }
        return latest.getGraceEndsAt();
    }

    private void saveNext(UUID studentId, ProfileSnapshot snap, LocalDateTime grace, StudentProfileVersionStatus status) {
        int number = versionRepo.findFirstByStudentIdOrderByVersionNumberDesc(studentId)
                .map(row -> row.getVersionNumber() + 1)
                .orElse(1);
        StudentProfileVersionEnt row = new StudentProfileVersionEnt();
        row.setStudentId(studentId);
        row.setVersionNumber(number);
        row.setStatus(status);
        row.setSnapshot(write(snap));
        row.setCreatedAt(now());
        row.setGraceEndsAt(grace);
        versionRepo.save(row);
    }

    private Optional<ProfileSnapshot> unpublished(UUID studentId) {
        return versionRepo.findFirstByStudentIdOrderByVersionNumberDesc(studentId)
                .filter(row -> row.getStatus() != StudentProfileVersionStatus.APPROVED)
                .map(this::read);
    }

    private StudentProfileVersionEnt latestApproved(UUID studentId) {
        return versionRepo.findByStudentIdOrderByVersionNumberAsc(studentId).stream()
                .filter(row -> row.getStatus() == StudentProfileVersionStatus.APPROVED)
                .max(Comparator.comparingInt(StudentProfileVersionEnt::getVersionNumber))
                .orElseThrow(() -> new NotFoundException("Одобренной анкеты ещё нет"));
    }

    private boolean latestIsDraft(UUID studentId) {
        return versionRepo.findFirstByStudentIdOrderByVersionNumberDesc(studentId)
                .filter(row -> row.getStatus() == StudentProfileVersionStatus.DRAFT)
                .isPresent();
    }

    private boolean seesDraftLists(UUID studentId) {
        if (studentId == null || isRecruiter() || unpublished(studentId).isEmpty()) {
            return false;
        }
        return isAdmin() || isOwnStudent(studentId);
    }

    private boolean viewerSeesDraft(UUID studentId) {
        return (isAdmin() || isOwnStudent(studentId)) && unpublished(studentId).isPresent();
    }

    private void markPendingIfApproved(UUID studentId) {
        userRepo.findByStudent_Id(studentId).ifPresent(user -> {
            if (user.getAccountStatus() == AccountStatus.APPROVED) {
                user.setAccountStatus(AccountStatus.PENDING_APPROVAL);
                userRepo.save(user);
            }
        });
    }

    private StudentEnt currentLive() {
        UserEnt user = userRepo.findByUsernameFetchingLinks(currentUsername())
                .orElseThrow(() -> new NotFoundException("К аккаунту не привязана карточка студента"));
        if (user.getStudent() == null) {
            throw new NotFoundException("К аккаунту не привязана карточка студента");
        }
        return user.getStudent();
    }

    private void requireOwn(UUID studentId) {
        if (!isOwnStudent(studentId)) {
            throw new NotFoundException("Версия не найдена");
        }
    }

    public boolean canReadDraftImage(UUID studentId) {
        return isAdmin() || isOwnStudent(studentId);
    }

    private boolean isOwnStudent(UUID studentId) {
        return securityHelper.getCurrentRoleOptional()
                .filter(role -> RoleEnum.STUDENT.getRole().equals(role))
                .isPresent()
                && userRepo.findByUsernameFetchingLinks(currentUsername())
                .filter(user -> user.getStudent() != null && studentId.equals(user.getStudent().getId()))
                .isPresent();
    }

    private String currentUsername() {
        return securityHelper.getCurrentUsernameOptional().orElse("");
    }

    private boolean isStudent() {
        return securityHelper.getCurrentRoleOptional().filter(role -> RoleEnum.STUDENT.getRole().equals(role)).isPresent();
    }

    private boolean isAdmin() {
        return securityHelper.isCurrentUserAdmin();
    }

    private boolean isRecruiter() {
        return securityHelper.getCurrentRoleOptional().filter(role -> RoleEnum.RECRUITER.getRole().equals(role)).isPresent();
    }

    private ProfileSnapshot capture(StudentEnt student) {
        ProfileSnapshot snap = new ProfileSnapshot();
        snap.setStudentId(student.getId());
        snap.setCity(student.getCity());
        snap.setHhLink(student.getHhLink());
        snap.setBirthDate(student.getBirthDate());
        snap.setBio(student.getBio());
        snap.setImagePath(student.getImagePath());
        snap.setCourse(student.getCourse());
        snap.setBusyness(student.getBusyness());
        snap.setDesiredBusyness(student.getDesiredBusyness());
        snap.setJobSearchStatus(student.getJobSearchStatus());
        snap.setMiddleName(student.getMiddleName());
        snap.setGender(student.getGender());
        snap.setPublicProfileConsent(student.isPublicProfileConsent());
        if (student.getUserInformation() != null) {
            snap.setFirstName(student.getUserInformation().getFirstName());
            snap.setLastName(student.getUserInformation().getLastName());
            snap.setEmail(student.getUserInformation().getEmail());
        }
        if (student.getContactInformation() != null) {
            snap.setPhoneNumber(student.getContactInformation().getPhoneNumber());
            snap.setTelegramUsername(student.getContactInformation().getTelegramUsername());
        }
        if (student.getSpeciality() != null) {
            snap.setSpecialityId(student.getSpeciality().getId());
            snap.setSpeciality(student.getSpeciality().getName());
        }
        if (student.getSkills() != null) {
            snap.setSkills(new ArrayList<>(student.getSkills().stream().map(skill -> {
                ProfileSnapshot.SkillSnap row = new ProfileSnapshot.SkillSnap();
                row.setId(skill.getId());
                row.setName(skill.getName());
                return row;
            }).toList()));
        }
        snap.setExperience(new ArrayList<>(experienceRepo.findAllByStudent_Id(student.getId()).stream().map(this::experienceSnap).toList()));
        snap.setEducation(new ArrayList<>(institutionRepo.findAllByStudent_Id(student.getId()).stream().map(this::educationSnap).toList()));
        snap.setPortfolio(new ArrayList<>(portfolioRepo.findAllByStudent_Id(student.getId()).stream().map(this::portfolioSnap).toList()));
        return snap;
    }

    private ProfileSnapshot.ExperienceSnap experienceSnap(ExperienceEnt entity) {
        ProfileSnapshot.ExperienceSnap row = new ProfileSnapshot.ExperienceSnap();
        row.setId(entity.getId() == null ? 0 : entity.getId());
        row.setCompanyId(entity.getCompany() == null ? 0 : entity.getCompany().getId());
        row.setCompanyWebsite(entity.getCompany() == null ? null : entity.getCompany().getWebsite());
        row.setPosition(entity.getPosition());
        row.setAdditionalInfo(entity.getAdditionalInfo());
        row.setStartDate(entity.getStartDate());
        row.setEndDate(entity.getEndDate());
        row.setExperienceType(entity.getExperienceType());
        return row;
    }

    private ProfileSnapshot.EducationSnap educationSnap(InstitutionEnt entity) {
        ProfileSnapshot.EducationSnap row = new ProfileSnapshot.EducationSnap();
        row.setId(entity.getId() == null ? 0 : entity.getId());
        row.setEducationId(entity.getEducation() == null ? 0 : entity.getEducation().getId());
        row.setStartYear(entity.getStartYear());
        row.setEndYear(entity.getEndYear());
        row.setEducationType(entity.getEducationType());
        row.setDirection(entity.getDirection());
        row.setEducationLevel(entity.getEducationLevel());
        return row;
    }

    private ProfileSnapshot.PortfolioSnap portfolioSnap(PortfolioEnt entity) {
        ProfileSnapshot.PortfolioSnap row = new ProfileSnapshot.PortfolioSnap();
        row.setId(entity.getId() == null ? 0 : entity.getId());
        row.setName(entity.getName());
        row.setLink(entity.getLink());
        row.setAdditionalInfo(entity.getAdditionalInfo());
        return row;
    }

    private void copyOnto(StudentEnt student, ProfileSnapshot snap) {
        student.setCity(snap.getCity());
        student.setHhLink(snap.getHhLink());
        student.setBirthDate(snap.getBirthDate());
        student.setBio(snap.getBio());
        student.setImagePath(snap.getImagePath());
        student.setCourse(snap.getCourse());
        student.setBusyness(snap.getBusyness());
        student.setDesiredBusyness(snap.getDesiredBusyness());
        student.setJobSearchStatus(snap.getJobSearchStatus());
        student.setMiddleName(snap.getMiddleName());
        student.setGender(snap.getGender());
        student.setPublicProfileConsent(snap.isPublicProfileConsent());
        UserInformation info = student.getUserInformation() == null ? new UserInformation() : student.getUserInformation();
        info.setFirstName(snap.getFirstName());
        info.setLastName(snap.getLastName());
        info.setEmail(snap.getEmail());
        student.setUserInformation(info);
        ContactInformation contact = student.getContactInformation() == null ? new ContactInformation() : student.getContactInformation();
        contact.setPhoneNumber(snap.getPhoneNumber());
        contact.setTelegramUsername(snap.getTelegramUsername());
        student.setContactInformation(contact);
        if (snap.getSpecialityId() != null) {
            student.setSpeciality(specialityRepo.findById(snap.getSpecialityId()).orElse(null));
        }
        if (snap.getSkills() != null) {
            List<Long> ids = snap.getSkills().stream().map(ProfileSnapshot.SkillSnap::getId).toList();
            student.setSkills(new HashSet<>(skillRepo.findAllById(ids)));
        }
        StudentProfileScoring.applyTo(student);
        studentRepo.save(student);
        experienceRepo.deleteByStudent_Id(student.getId());
        institutionRepo.deleteByStudent_Id(student.getId());
        portfolioRepo.deleteByStudent_Id(student.getId());
        for (ProfileSnapshot.ExperienceSnap item : snap.getExperience()) {
            ExperienceEnt entity = new ExperienceEnt();
            entity.setPosition(item.getPosition());
            entity.setAdditionalInfo(item.getAdditionalInfo());
            entity.setStartDate(item.getStartDate());
            entity.setEndDate(item.getEndDate());
            entity.setExperienceType(item.getExperienceType() == null ? ExperienceType.WORK : item.getExperienceType());
            entity.setStudent(student);
            entity.setCompany(companyRepo.findById(item.getCompanyId()).orElseThrow(() -> new NotFoundException("Компания не найдена")));
            experienceRepo.save(entity);
        }
        for (ProfileSnapshot.EducationSnap item : snap.getEducation()) {
            InstitutionEnt entity = new InstitutionEnt();
            entity.setStartYear(item.getStartYear());
            entity.setEndYear(item.getEndYear());
            entity.setEducationType(item.getEducationType() == null ? EducationType.STATE : item.getEducationType());
            entity.setDirection(item.getDirection());
            entity.setEducationLevel(item.getEducationLevel());
            entity.setStudent(student);
            entity.setEducation(educationRepo.findById(item.getEducationId()).orElseThrow(() -> new NotFoundException("Учебное заведение не найдено")));
            institutionRepo.save(entity);
        }
        for (ProfileSnapshot.PortfolioSnap item : snap.getPortfolio()) {
            PortfolioEnt entity = new PortfolioEnt();
            entity.setName(item.getName());
            entity.setLink(item.getLink());
            entity.setAdditionalInfo(item.getAdditionalInfo());
            entity.setStudent(student);
            portfolioRepo.save(entity);
        }
    }

    private void applyCard(ProfileSnapshot snap, PatchStudentMeReq req) {
        if (req.city() != null) snap.setCity(req.city());
        if (req.hhLink() != null) snap.setHhLink(req.hhLink());
        if (req.birthDate() != null) snap.setBirthDate(req.birthDate());
        if (req.bio() != null) snap.setBio(req.bio());
        if (req.course() != null) snap.setCourse(req.course());
        if (req.busyness() != null) snap.setBusyness(req.busyness());
        if (req.desiredBusyness() != null) snap.setDesiredBusyness(req.desiredBusyness());
        if (req.jobSearchStatus() != null) snap.setJobSearchStatus(req.jobSearchStatus());
        if (req.firstName() != null) snap.setFirstName(req.firstName());
        if (req.lastName() != null) snap.setLastName(req.lastName());
        if (req.middleName() != null) snap.setMiddleName(req.middleName());
        if (req.gender() != null) snap.setGender(req.gender());
        if (req.email() != null) snap.setEmail(req.email());
        if (req.phoneNumber() != null) snap.setPhoneNumber(req.phoneNumber());
        if (req.telegramUsername() != null) snap.setTelegramUsername(req.telegramUsername());
        if (req.specialityId() != null) {
            snap.setSpecialityId(req.specialityId());
            specialityRepo.findById(req.specialityId()).ifPresent(item -> snap.setSpeciality(item.getName()));
        }
        if (req.skillsIds() != null) {
            snap.setSkills(skillRepo.findAllById(req.skillsIds()).stream().map(this::skillSnap).toList());
        }
        if (req.publicProfileConsent() != null) snap.setPublicProfileConsent(req.publicProfileConsent());
    }

    private void applyAdminCard(ProfileSnapshot snap, PatchStudentReq req) {
        if (req.city() != null) snap.setCity(req.city());
        if (req.hhLink() != null) snap.setHhLink(req.hhLink());
        if (req.birthDate() != null) snap.setBirthDate(req.birthDate());
        if (req.bio() != null) snap.setBio(req.bio());
        if (req.course() != null) snap.setCourse(req.course());
        if (req.busyness() != null) snap.setBusyness(req.busyness());
        if (req.desiredBusyness() != null) snap.setDesiredBusyness(req.desiredBusyness());
        if (req.jobSearchStatus() != null) snap.setJobSearchStatus(req.jobSearchStatus());
        if (req.firstName() != null) snap.setFirstName(req.firstName());
        if (req.lastName() != null) snap.setLastName(req.lastName());
        if (req.middleName() != null) snap.setMiddleName(req.middleName());
        if (req.gender() != null) snap.setGender(req.gender());
        if (req.email() != null) snap.setEmail(req.email());
        if (req.phoneNumber() != null) snap.setPhoneNumber(req.phoneNumber());
        if (req.telegramUsername() != null) snap.setTelegramUsername(req.telegramUsername());
        if (req.specialityId() != null) {
            snap.setSpecialityId(req.specialityId());
            specialityRepo.findById(req.specialityId()).ifPresent(item -> snap.setSpeciality(item.getName()));
        }
        if (req.skillsIds() != null) {
            snap.setSkills(skillRepo.findAllById(req.skillsIds()).stream().map(this::skillSnap).toList());
        }
        if (req.publicProfileConsent() != null) snap.setPublicProfileConsent(req.publicProfileConsent());
    }

    private ProfileSnapshot.SkillSnap skillSnap(SkillEnt skill) {
        ProfileSnapshot.SkillSnap row = new ProfileSnapshot.SkillSnap();
        row.setId(skill.getId());
        row.setName(skill.getName());
        return row;
    }

    private StudentDTO toCard(ProfileSnapshot snap, StudentEnt live) {
        List<SkillDTO> skills = snap.getSkills() == null ? List.of() : snap.getSkills().stream()
                .map(skill -> new SkillDTO(skill.getId(), skill.getName())).toList();
        return new StudentDTO(
                live.getId(),
                snap.getCity(),
                snap.getHhLink(),
                snap.getBirthDate(),
                snap.getBio(),
                snap.getImagePath(),
                snap.getCourse(),
                snap.getBusyness(),
                snap.getDesiredBusyness(),
                snap.getJobSearchStatus(),
                snap.getFirstName(),
                snap.getLastName(),
                snap.getMiddleName(),
                snap.getGender(),
                snap.getEmail(),
                snap.getPhoneNumber(),
                snap.getTelegramUsername(),
                snap.getSpecialityId(),
                snap.getSpeciality(),
                skills,
                snap.isPublicProfileConsent(),
                live.isCatalogVisible(),
                live.getProfileTextScore(),
                live.getManualSortOrder());
    }

    private List<String> problems(ProfileSnapshot snap) {
        List<String> problems = new ArrayList<>();
        if (blank(snap.getImagePath())) problems.add("нет фото");
        if (snap.getPortfolio() == null || snap.getPortfolio().isEmpty()) problems.add("нет портфолио");
        if (snap.getExperience() == null || snap.getExperience().isEmpty()) problems.add("нет опыта");
        if (snap.getEducation() == null || snap.getEducation().isEmpty()) problems.add("нет образования");
        if (snap.getBio() == null || snap.getBio().trim().length() < 30) problems.add("описание короче 30 символов");
        if (blank(snap.getFirstName())) problems.add("нет имени");
        if (blank(snap.getLastName())) problems.add("нет фамилии");
        if (snap.getSpecialityId() == null) problems.add("нет специальности");
        if (snap.getCourse() == null) problems.add("нет курса");
        if (snap.getSkills() == null || snap.getSkills().size() < 5) problems.add("навыков меньше 5");
        if (snap.getBusyness() == null) problems.add("нет типа занятости");
        if (blank(snap.getCity())) problems.add("нет города");
        return problems;
    }

    private String publishedImage(UUID studentId) {
        if (!hasPublishedVersion(studentId)) {
            return null;
        }
        return read(latestApproved(studentId)).getImagePath();
    }

    private void deleteDraftFile(String previous, String published, String next) {
        if (previous == null || previous.isBlank() || previous.equals(published) || previous.equals(next)) {
            return;
        }
    }

    private String website(long companyId) {
        return companyRepo.findById(companyId).map(CompanyEnt::getWebsite).orElse(null);
    }

    private ExperienceDTO experienceDto(UUID studentId, ProfileSnapshot.ExperienceSnap item) {
        return new ExperienceDTO(item.getCompanyId(), item.getCompanyWebsite(), studentId,
                new ExperienceRes(item.getId(), item.getPosition(), item.getAdditionalInfo(),
                        item.getStartDate(), item.getEndDate(),
                        item.getExperienceType() == null ? ExperienceType.WORK : item.getExperienceType()));
    }

    private InstitutionDTO educationDto(UUID studentId, ProfileSnapshot.EducationSnap item) {
        return new InstitutionDTO(item.getEducationId(), studentId,
                new InstitutionRes(item.getId(), item.getStartYear(), item.getEndYear(),
                        item.getEducationType() == null ? EducationType.STATE : item.getEducationType(),
                        item.getDirection(), item.getEducationLevel()));
    }

    private static long nextItemId(List<Long> ids) {
        return ids.stream().mapToLong(Long::longValue).max().orElse(0) + 1;
    }

    private <T> PageResponse<T> page(List<T> rows, Pageable pageable) {
        int page = pageable == null ? 0 : pageable.getPageNumber();
        int size = pageable == null || pageable.getPageSize() <= 0 ? Math.max(rows.size(), 1) : pageable.getPageSize();
        int from = Math.min(page * size, rows.size());
        int to = Math.min(from + size, rows.size());
        int pages = size == 0 ? 0 : (int) Math.ceil(rows.size() / (double) size);
        return new PageResponse<>(rows.subList(from, to), page, size, rows.size(), pages);
    }

    private ProfileSnapshot read(StudentProfileVersionEnt row) {
        try {
            ProfileSnapshot snap = objectMapper.readValue(row.getSnapshot(), ProfileSnapshot.class);
            if (snap.getSkills() == null) snap.setSkills(List.of());
            if (snap.getExperience() == null) snap.setExperience(new ArrayList<>());
            if (snap.getEducation() == null) snap.setEducation(new ArrayList<>());
            if (snap.getPortfolio() == null) snap.setPortfolio(new ArrayList<>());
            return snap;
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Не удалось прочитать версию анкеты", ex);
        }
    }

    private String write(ProfileSnapshot snap) {
        try {
            return objectMapper.writeValueAsString(snap);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Не удалось записать версию анкеты", ex);
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }
}
