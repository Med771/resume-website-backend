package ru.ai.sin.logic.siteproject;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.siteproject.dto.FilterSiteProjectReq;
import ru.ai.sin.logic.siteproject.dto.SiteProjectDTO;
import ru.ai.sin.logic.siteproject.dto.SiteProjectImageReq;
import ru.ai.sin.logic.siteproject.dto.SiteProjectStudentsReq;
import ru.ai.sin.logic.siteproject.dto.UpdateSiteProjectReq;
import ru.ai.sin.logic.skill.SkillMapper;
import ru.ai.sin.logic.skill.SkillRepo;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SiteProjectServiceImplTest {

    @Mock
    private SiteProjectRepo siteProjectRepo;
    @Mock
    private StudentRepo studentRepo;
    @Mock
    private SkillRepo skillRepo;
    @Mock
    private SkillMapper skillMapper;
    @Mock
    private SecurityHelper securityHelper;

    private SiteProjectServiceImpl service;

    private final UUID projectId = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa");
    private final UUID studentId1 = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb");
    private final UUID studentId2 = UUID.fromString("cccccccc-cccc-cccc-cccc-cccccccccccc");

    @BeforeEach
    void setUp() {
        service = new SiteProjectServiceImpl(siteProjectRepo, studentRepo, skillRepo, skillMapper, securityHelper);
    }

    @Test
    void listForVitrina_onlyAnonymousFlagInPublicationWindow() {
        SiteProjectEnt publicProject = project("Public", true, null, null);
        SiteProjectEnt authOnly = project("Auth only", false, null, null);
        SiteProjectEnt expired = project("Expired", true, null, LocalDateTime.now().minusDays(1));

        when(siteProjectRepo.findAllWithImagesByOrderBySortOrderAsc()).thenReturn(List.of(publicProject, authOnly, expired));

        assertThat(service.listForVitrina(10))
                .extracting(SiteProjectDTO::title)
                .containsExactly("Public");
    }

    @Test
    void listForVitrina_respectsLimit() {
        SiteProjectEnt first = project("First", true, null, null);
        SiteProjectEnt second = project("Second", true, null, null);
        when(siteProjectRepo.findAllWithImagesByOrderBySortOrderAsc()).thenReturn(List.of(first, second));

        assertThat(service.listForVitrina(1))
                .extracting(SiteProjectDTO::title)
                .containsExactly("First");
    }

    @Test
    void filter_student_allInPublicationWindowRegardlessOfAnonymousFlag() {
        asStudent();
        SiteProjectEnt publicProject = project("Public", true, null, null);
        SiteProjectEnt authOnly = project("Auth only", false, null, null);
        SiteProjectEnt expired = project("Expired", true, null, LocalDateTime.now().minusDays(1));

        when(siteProjectRepo.findAllWithImagesByOrderBySortOrderAsc()).thenReturn(List.of(publicProject, authOnly, expired));

        assertThat(service.filter(null))
                .extracting(SiteProjectDTO::title)
                .containsExactly("Public", "Auth only");
    }

    @Test
    void filter_recruiter_includesStudents() {
        asRecruiter();
        SiteProjectEnt publicProject = project("Public", true, null, null);
        when(siteProjectRepo.findAllWithDetailsByOrderBySortOrderAsc()).thenReturn(List.of(publicProject));

        assertThat(service.filter(null))
                .singleElement()
                .satisfies(dto -> assertThat(dto.students()).isNotNull());
    }

    @Test
    void filter_student_excludesStudents() {
        asStudent();
        SiteProjectEnt publicProject = project("Public", true, null, null);
        when(siteProjectRepo.findAllWithImagesByOrderBySortOrderAsc()).thenReturn(List.of(publicProject));

        assertThat(service.filter(null))
                .singleElement()
                .satisfies(dto -> assertThat(dto.students()).isNull());
    }

    @Test
    void filter_admin_includesOutOfWindow() {
        asAdmin();
        SiteProjectEnt expired = project("Expired", true, null, LocalDateTime.now().minusDays(1));
        when(siteProjectRepo.findAllWithDetailsByOrderBySortOrderAsc()).thenReturn(List.of(expired));

        assertThat(service.filter(null))
                .extracting(SiteProjectDTO::title)
                .containsExactly("Expired");
    }

    @Test
    void filter_qFiltersBySection() {
        asStudent();
        SiteProjectEnt match = project("Web app", true, null, null);
        match.setSection("Веб-разработка");
        match.setSummary("React dashboard");
        SiteProjectEnt other = project("Other", true, null, null);
        other.setSection("Игры");

        when(siteProjectRepo.findAllWithImagesByOrderBySortOrderAsc()).thenReturn(List.of(match, other));

        assertThat(service.filter(new FilterSiteProjectReq("веб", null, null)))
                .extracting(SiteProjectDTO::title)
                .containsExactly("Web app");
    }

    @Test
    void filter_sectionAndVisibleToAnonymous() {
        asAdmin();
        SiteProjectEnt match = project("Shown", true, null, null);
        match.setSection("games");
        SiteProjectEnt hidden = project("Hidden", false, null, null);
        hidden.setSection("games");
        SiteProjectEnt otherSection = project("Other", true, null, null);
        otherSection.setSection("web");

        when(siteProjectRepo.findAllWithDetailsByOrderBySortOrderAsc()).thenReturn(List.of(match, hidden, otherSection));

        assertThat(service.filter(new FilterSiteProjectReq(null, "game", true)))
                .extracting(SiteProjectDTO::title)
                .containsExactly("Shown");
    }

    @Test
    void getById_deduplicatesImagesFromPersistenceBag() {
        asAdmin();
        SiteProjectEnt project = project("Gallery", true, null, null);
        project.setId(projectId);
        SiteProjectImageEnt image = new SiteProjectImageEnt();
        UUID imageId = UUID.randomUUID();
        image.setId(imageId);
        image.setProject(project);
        image.setImagePath("photo.jpg");
        image.setSortOrder(0);
        project.getImages().add(image);
        project.getImages().add(image);

        when(siteProjectRepo.findWithDetailsById(projectId)).thenReturn(Optional.of(project));

        assertThat(service.getById(projectId).images())
                .hasSize(1)
                .first()
                .satisfies(dto -> assertThat(dto.id()).isEqualTo(imageId));
    }

    @Test
    void getById_outOfWindow_404ForStudent() {
        asStudent();
        SiteProjectEnt expired = project("Expired", true, null, LocalDateTime.now().minusDays(1));
        expired.setId(projectId);
        when(siteProjectRepo.findWithImagesById(projectId)).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> service.getById(projectId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getById_adminSeesOutOfWindow() {
        asAdmin();
        SiteProjectEnt expired = project("Expired", true, null, LocalDateTime.now().minusDays(1));
        expired.setId(projectId);
        when(siteProjectRepo.findWithDetailsById(projectId)).thenReturn(Optional.of(expired));

        assertThat(service.getById(projectId).title()).isEqualTo("Expired");
    }

    private void asAdmin() {
        when(securityHelper.isCurrentUserAdmin()).thenReturn(true);
    }

    private void asStudent() {
        when(securityHelper.isCurrentUserAdmin()).thenReturn(false);
        when(securityHelper.getCurrentRoleOptional()).thenReturn(Optional.of("STUDENT"));
    }

    private void asRecruiter() {
        when(securityHelper.isCurrentUserAdmin()).thenReturn(false);
        when(securityHelper.getCurrentRoleOptional()).thenReturn(Optional.of("RECRUITER"));
    }

    @Test
    void update_deduplicatesDuplicateImagePayload() {
        SiteProjectEnt project = project("Gallery", true, null, null);
        project.setId(projectId);
        when(siteProjectRepo.findWithImagesById(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRepo.save(project)).thenReturn(project);

        service.update(projectId, new UpdateSiteProjectReq(
                "Gallery",
                null,
                null,
                null,
                List.of(
                        new SiteProjectImageReq(null, "a.jpg", null, 0),
                        new SiteProjectImageReq(null, "a.jpg", null, 1)
                ),
                List.of(),
                true,
                null,
                null
        ));

        assertThat(project.getImages()).hasSize(1);
        assertThat(project.getImages().getFirst().getImagePath()).isEqualTo("a.jpg");
    }

    @Test
    void listStudentIds_projectNotFound() {
        when(siteProjectRepo.existsById(projectId)).thenReturn(false);

        assertThatThrownBy(() -> service.listStudentIds(projectId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void listStudentIds_returnsIdsFromRepo() {
        when(siteProjectRepo.existsById(projectId)).thenReturn(true);
        when(siteProjectRepo.findStudentIdsByProjectId(projectId)).thenReturn(List.of(studentId1));

        assertThat(service.listStudentIds(projectId)).containsExactly(studentId1);
    }

    @Test
    void bindStudents_duplicateIdsInRequest() {
        assertThatThrownBy(() -> service.bindStudents(
                projectId, new SiteProjectStudentsReq(List.of(studentId1, studentId1))))
                .isInstanceOf(BadRequestException.class);

        verify(siteProjectRepo, never()).findWithStudentsById(any());
    }

    @Test
    void bindStudents_projectNotFound() {
        when(siteProjectRepo.findWithStudentsById(projectId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.bindStudents(
                projectId, new SiteProjectStudentsReq(List.of(studentId1))))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void bindStudents_studentNotFound() {
        SiteProjectEnt project = new SiteProjectEnt();
        project.setId(projectId);
        when(siteProjectRepo.findWithStudentsById(projectId)).thenReturn(Optional.of(project));
        when(studentRepo.findAllById(List.of(studentId1))).thenReturn(List.of());

        assertThatThrownBy(() -> service.bindStudents(
                projectId, new SiteProjectStudentsReq(List.of(studentId1))))
                .isInstanceOf(NotFoundException.class);

        verify(siteProjectRepo, never()).save(any());
    }

    @Test
    void bindStudents_addsStudentsAndSaves() {
        SiteProjectEnt project = new SiteProjectEnt();
        project.setId(projectId);
        StudentEnt s1 = new StudentEnt();
        s1.setId(studentId1);
        StudentEnt s2 = new StudentEnt();
        s2.setId(studentId2);

        when(siteProjectRepo.findWithStudentsById(projectId)).thenReturn(Optional.of(project));
        when(studentRepo.findAllById(List.of(studentId1, studentId2))).thenReturn(List.of(s1, s2));
        when(siteProjectRepo.save(project)).thenReturn(project);

        service.bindStudents(projectId, new SiteProjectStudentsReq(List.of(studentId1, studentId2)));

        assertThat(project.getStudents()).containsExactlyInAnyOrder(s1, s2);
        verify(siteProjectRepo).save(project);
    }

    @Test
    void unbindStudents_removesOnlyListedStudents() {
        SiteProjectEnt project = new SiteProjectEnt();
        project.setId(projectId);
        StudentEnt s1 = new StudentEnt();
        s1.setId(studentId1);
        StudentEnt s2 = new StudentEnt();
        s2.setId(studentId2);
        project.setStudents(new java.util.HashSet<>(Set.of(s1, s2)));

        when(siteProjectRepo.findWithStudentsById(projectId)).thenReturn(Optional.of(project));
        when(siteProjectRepo.save(project)).thenReturn(project);

        service.unbindStudents(projectId, new SiteProjectStudentsReq(List.of(studentId1)));

        assertThat(project.getStudents()).containsExactly(s2);
        verify(siteProjectRepo).save(project);
    }

    private static SiteProjectEnt project(String title, boolean visibleToAnonymous,
                                          LocalDateTime publishedFrom, LocalDateTime publishedTo) {
        SiteProjectEnt e = new SiteProjectEnt();
        e.setId(UUID.randomUUID());
        e.setTitle(title);
        e.setVisibleToAnonymous(visibleToAnonymous);
        e.setPublishedFrom(publishedFrom);
        e.setPublishedTo(publishedTo);
        e.setSortOrder(0);
        return e;
    }
}
