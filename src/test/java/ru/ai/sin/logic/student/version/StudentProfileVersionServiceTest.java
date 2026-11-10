package ru.ai.sin.logic.student.version;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.company.CompanyRepo;
import ru.ai.sin.logic.education.EducationRepo;
import ru.ai.sin.logic.experience.ExperienceRepo;
import ru.ai.sin.logic.institution.InstitutionRepo;
import ru.ai.sin.logic.portfolio.PortfolioRepo;
import ru.ai.sin.logic.skill.SkillRepo;
import ru.ai.sin.logic.speciality.SpecialityRepo;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.student.dto.PatchStudentMeReq;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.models.enums.AccountStatus;
import ru.ai.sin.models.enums.RoleEnum;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentProfileVersionServiceTest {

    private static final UUID STUDENT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private static final ZoneId ZONE = ZoneId.of("UTC");

    @Mock
    private StudentProfileVersionRepo versionRepo;
    @Mock
    private StudentRepo studentRepo;
    @Mock
    private UserRepo userRepo;
    @Mock
    private ExperienceRepo experienceRepo;
    @Mock
    private InstitutionRepo institutionRepo;
    @Mock
    private PortfolioRepo portfolioRepo;
    @Mock
    private SkillRepo skillRepo;
    @Mock
    private SpecialityRepo specialityRepo;
    @Mock
    private CompanyRepo companyRepo;
    @Mock
    private EducationRepo educationRepo;
    @Mock
    private SecurityHelper securityHelper;

    private final List<StudentProfileVersionEnt> rows = new ArrayList<>();
    private Clock clock;
    private StudentProfileVersionService service;
    private StudentEnt student;
    private UserEnt user;

    @BeforeEach
    void setUp() {
        clock = Clock.fixed(Instant.parse("2026-11-08T12:00:00Z"), ZONE);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        service = new StudentProfileVersionService(
                versionRepo, studentRepo, userRepo, experienceRepo, institutionRepo, portfolioRepo,
                skillRepo, specialityRepo, companyRepo, educationRepo, securityHelper, mapper, clock);
        student = new StudentEnt();
        student.setId(STUDENT_ID);
        student.setBio("old bio");
        student.setCatalogVisible(true);
        user = new UserEnt(RoleEnum.STUDENT, "s", "student", "hash");
        user.setStudent(student);
        user.setAccountStatus(AccountStatus.APPROVED);
        lenient().when(versionRepo.save(any())).thenAnswer(invocation -> {
            StudentProfileVersionEnt row = invocation.getArgument(0);
            if (row.getId() == null) {
                row.setId(UUID.randomUUID());
            }
            rows.removeIf(existing -> existing.getStudentId().equals(row.getStudentId())
                    && existing.getVersionNumber() == row.getVersionNumber());
            rows.add(row);
            return row;
        });
        lenient().when(versionRepo.findFirstByStudentIdOrderByVersionNumberDesc(any())).thenAnswer(invocation -> latest(invocation.getArgument(0)));
        lenient().when(versionRepo.findByStudentIdOrderByVersionNumberAsc(any())).thenAnswer(invocation -> rows.stream()
                .filter(row -> row.getStudentId().equals(invocation.getArgument(0)))
                .sorted(Comparator.comparingInt(StudentProfileVersionEnt::getVersionNumber))
                .toList());
        lenient().when(versionRepo.findByStudentIdAndVersionNumber(any(), anyInt())).thenAnswer(invocation -> rows.stream()
                .filter(row -> row.getStudentId().equals(invocation.getArgument(0))
                        && row.getVersionNumber() == invocation.getArgument(1, Integer.class))
                .findFirst());
        lenient().when(versionRepo.existsByStudentIdAndStatus(any(), any())).thenAnswer(invocation -> rows.stream()
                .anyMatch(row -> row.getStudentId().equals(invocation.getArgument(0))
                        && row.getStatus() == invocation.getArgument(1)));
        lenient().when(versionRepo.findByStatusAndGraceEndsAtBefore(any(), any())).thenAnswer(invocation -> rows.stream()
                .filter(row -> row.getStatus() == invocation.getArgument(0)
                        && row.getGraceEndsAt() != null
                        && row.getGraceEndsAt().isBefore(invocation.getArgument(1)))
                .toList());
        lenient().doAnswer(invocation -> {
            UUID studentId = invocation.getArgument(0);
            StudentProfileVersionStatus status = invocation.getArgument(1);
            rows.removeIf(row -> row.getStudentId().equals(studentId) && row.getStatus() == status);
            return null;
        }).when(versionRepo).deleteByStudentIdAndStatus(any(), any());
        lenient().when(experienceRepo.findAllByStudent_Id(any())).thenReturn(List.of());
        lenient().when(institutionRepo.findAllByStudent_Id(any())).thenReturn(List.of());
        lenient().when(portfolioRepo.findAllByStudent_Id(any())).thenReturn(List.of());
        lenient().when(userRepo.findByStudent_Id(STUDENT_ID)).thenReturn(Optional.of(user));
        lenient().when(userRepo.findByUsernameFetchingLinks("student")).thenReturn(Optional.of(user));
        lenient().when(securityHelper.getCurrentRoleOptional()).thenReturn(Optional.of("STUDENT"));
        lenient().when(securityHelper.getCurrentUsernameOptional()).thenReturn(Optional.of("student"));
        lenient().when(securityHelper.isCurrentUserAdmin()).thenReturn(false);
    }

    @Test
    void beforeFirstApproval_publishedIsMissingAndGraceIsEmpty() {
        assertThat(service.hasPublishedVersion(STUDENT_ID)).isFalse();
        assertThatThrownBy(() -> service.publishedCard(student)).isInstanceOf(NotFoundException.class);
        assertThat(service.activeGraceEndsAt(STUDENT_ID)).isNull();
    }

    @Test
    void firstEditKeepsApprovedTextInCatalogAndOpensGrace() {
        service.publishOnApprove(student);

        boolean captured = service.captureCardPatch(STUDENT_ID, patch("new bio"));

        assertThat(captured).isTrue();
        assertThat(student.getBio()).isEqualTo("old bio");
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.APPROVED);
        assertThat(service.activeGraceEndsAt(STUDENT_ID)).isEqualTo(LocalDateTime.now(clock).plusMinutes(5));
        assertThat(service.draftCard(student).orElseThrow().bio()).isEqualTo("new bio");
        assertThat(service.publishedCard(student).bio()).isEqualTo("old bio");
    }

    @Test
    void secondEditDoesNotMoveGrace() {
        service.publishOnApprove(student);
        service.captureCardPatch(STUDENT_ID, patch("new bio"));
        LocalDateTime grace = service.activeGraceEndsAt(STUDENT_ID);

        service.captureCardPatch(STUDENT_ID, patch("newer bio"));

        assertThat(service.activeGraceEndsAt(STUDENT_ID)).isEqualTo(grace);
        assertThat(service.draftCard(student).orElseThrow().bio()).isEqualTo("newer bio");
    }

    @Test
    void revertInsideGraceDeletesDraftAndKeepsApproval() {
        service.publishOnApprove(student);
        service.captureCardPatch(STUDENT_ID, patch("new bio"));

        service.revertDraft(STUDENT_ID);

        assertThat(service.draftCard(student)).isEmpty();
        assertThat(service.activeGraceEndsAt(STUDENT_ID)).isNull();
        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.APPROVED);
        assertThat(service.publishedCard(student).bio()).isEqualTo("old bio");
    }

    @Test
    void revertAfterGraceDoesNotRestoreApproved() {
        service.publishOnApprove(student);
        service.captureCardPatch(STUDENT_ID, patch("new bio"));
        clock = Clock.fixed(Instant.parse("2026-11-08T12:06:00Z"), ZONE);
        service = rebuild();

        service.revertDraft(STUDENT_ID);

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        assertThat(service.draftCard(student).orElseThrow().bio()).isEqualTo("new bio");
    }

    @Test
    void expiredGraceOnReadMarksPendingButPublishedStays() {
        service.publishOnApprove(student);
        service.captureCardPatch(STUDENT_ID, patch("new bio"));
        clock = Clock.fixed(Instant.parse("2026-11-08T12:06:00Z"), ZONE);
        service = rebuild();

        service.expireDueDrafts();

        assertThat(user.getAccountStatus()).isEqualTo(AccountStatus.PENDING_APPROVAL);
        assertThat(service.publishedCard(student).bio()).isEqualTo("old bio");
        assertThat(service.activeGraceEndsAt(STUDENT_ID)).isNull();
    }

    @Test
    void versionByNumberReturnsThatSnapshot() {
        service.publishOnApprove(student);
        service.captureCardPatch(STUDENT_ID, patch("new bio"));
        service.captureCardPatch(STUDENT_ID, patch("newer bio"));

        assertThat(service.ownVersion(student, 2).bio()).isEqualTo("new bio");
        assertThat(service.ownVersion(student, 3).bio()).isEqualTo("newer bio");
        assertThat(service.listOwn(STUDENT_ID)).extracting(item -> item.status())
                .contains("APPROVED", "DRAFT");
        assertThatThrownBy(() -> service.ownVersion(student, 99)).isInstanceOf(NotFoundException.class);
    }

    @Test
    void rejectKeepsPublishedAndMarksDraft() {
        service.publishOnApprove(student);
        service.captureCardPatch(STUDENT_ID, patch("new bio"));

        service.markLatestDraftRejected(STUDENT_ID);
        user.setAccountStatus(AccountStatus.REJECTED);

        assertThat(student.getBio()).isEqualTo("old bio");
        assertThat(service.publishedCard(student).bio()).isEqualTo("old bio");
        assertThat(service.draftCard(student).orElseThrow().bio()).isEqualTo("new bio");
        assertThat(service.activeGraceEndsAt(STUDENT_ID)).isNull();
        assertThat(service.listOwn(STUDENT_ID)).extracting(item -> item.status())
                .contains("APPROVED", "REJECTED");
    }

    @Test
    void editAfterRejectStartsNewGraceAndKeepsRejectedSnapshot() {
        service.publishOnApprove(student);
        service.captureCardPatch(STUDENT_ID, patch("rejected bio"));
        service.markLatestDraftRejected(STUDENT_ID);

        service.captureCardPatch(STUDENT_ID, patch("another bio"));

        assertThat(service.activeGraceEndsAt(STUDENT_ID)).isEqualTo(LocalDateTime.now(clock).plusMinutes(5));
        assertThat(service.listOwn(STUDENT_ID)).extracting(item -> item.status())
                .contains("REJECTED", "DRAFT");
    }

    @Test
    void approveCopiesDraftIntoLiveRow() {
        service.publishOnApprove(student);
        service.captureCardPatch(STUDENT_ID, patch("new bio that is definitely long enough"));
        when(studentRepo.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.publishOnApprove(student);

        assertThat(student.getBio()).isEqualTo("new bio that is definitely long enough");
        assertThat(service.activeGraceEndsAt(STUDENT_ID)).isNull();
        assertThat(service.hasPublishedVersion(STUDENT_ID)).isTrue();
    }

    @Test
    void otherStudentCannotOpenVersion() {
        service.publishOnApprove(student);
        when(securityHelper.getCurrentUsernameOptional()).thenReturn(Optional.of("other"));
        UserEnt other = new UserEnt(RoleEnum.STUDENT, "o", "other", "hash");
        StudentEnt otherStudent = new StudentEnt();
        otherStudent.setId(UUID.randomUUID());
        other.setStudent(otherStudent);
        when(userRepo.findByUsernameFetchingLinks("other")).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.listOwn(STUDENT_ID)).isInstanceOf(NotFoundException.class);
    }

    private StudentProfileVersionService rebuild() {
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule());
        return new StudentProfileVersionService(
                versionRepo, studentRepo, userRepo, experienceRepo, institutionRepo, portfolioRepo,
                skillRepo, specialityRepo, companyRepo, educationRepo, securityHelper, mapper, clock);
    }

    private Optional<StudentProfileVersionEnt> latest(UUID studentId) {
        return rows.stream()
                .filter(row -> row.getStudentId().equals(studentId))
                .max(Comparator.comparingInt(StudentProfileVersionEnt::getVersionNumber));
    }

    private static PatchStudentMeReq patch(String bio) {
        return new PatchStudentMeReq(
                null, null, null, bio, null, null, null, null,
                null, null, null, null, null, null, null, null,
                null, null, null);
    }
}
