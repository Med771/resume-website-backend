package ru.ai.sin.logic.main;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.exception.models.ForbiddenException;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.AccountAccessHelper;
import ru.ai.sin.helper.FileHelper;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MainServiceImplTest {

    private static final UUID STUDENT_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");

    @Mock
    private FileHelper fileHelper;
    @Mock
    private StudentRepo studentRepo;
    @Mock
    private AccountAccessHelper accountAccessHelper;
    @Mock
    private SecurityHelper securityHelper;

    @InjectMocks
    private MainServiceImpl service;

    @Test
    void adminReadsFileThatIsNotAnAvatar() throws Exception {
        when(studentRepo.findByImagePath("project.png")).thenReturn(Optional.empty());
        when(securityHelper.isCurrentUserAdmin()).thenReturn(true);
        when(fileHelper.getFileContent("project.png")).thenReturn(new byte[] {1});

        assertThat(service.getFileContent("project.png")).containsExactly(1);

        verify(accountAccessHelper, never()).requireCanReadStudentResumeDetails(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void studentReadsOwnAvatar() throws Exception {
        StudentEnt student = student("own.jpg");
        when(studentRepo.findByImagePath("own.jpg")).thenReturn(Optional.of(student));
        when(fileHelper.getFileContent("own.jpg")).thenReturn(new byte[] {2});

        assertThat(service.getFileContent("own.jpg")).containsExactly(2);

        verify(accountAccessHelper).requireCanReadStudentResumeDetails(STUDENT_ID);
    }

    @Test
    void hiddenAvatarIsNotRead() throws Exception {
        StudentEnt student = student("hidden.jpg");
        when(studentRepo.findByImagePath("hidden.jpg")).thenReturn(Optional.of(student));
        org.mockito.Mockito.doThrow(new NotFoundException("Failed to find student by id " + STUDENT_ID))
                .when(accountAccessHelper).requireCanReadStudentResumeDetails(STUDENT_ID);

        assertThatThrownBy(() -> service.getFileContent("hidden.jpg"))
                .isInstanceOf(NotFoundException.class);
        verify(fileHelper, never()).getFileContent("hidden.jpg");
    }

    @Test
    void recruiterWithoutApprovalIsForbidden() throws Exception {
        StudentEnt student = student("approved.jpg");
        when(studentRepo.findByImagePath("approved.jpg")).thenReturn(Optional.of(student));
        org.mockito.Mockito.doThrow(new ForbiddenException("Аккаунт ожидает одобрения администратором"))
                .when(accountAccessHelper).requireCanReadStudentResumeDetails(STUDENT_ID);

        assertThatThrownBy(() -> service.getFileContent("approved.jpg"))
                .isInstanceOf(ForbiddenException.class);
        verify(fileHelper, never()).getFileContent("approved.jpg");
    }

    @Test
    void nonAvatarIsHiddenFromNonAdmin() throws Exception {
        when(studentRepo.findByImagePath("chat-file.jpg")).thenReturn(Optional.empty());
        when(securityHelper.isCurrentUserAdmin()).thenReturn(false);

        assertThatThrownBy(() -> service.getFileContent("chat-file.jpg"))
                .isInstanceOf(NotFoundException.class);
        verify(fileHelper, never()).getFileContent(org.mockito.ArgumentMatchers.anyString());
    }

    private static StudentEnt student(String imagePath) {
        StudentEnt student = new StudentEnt();
        student.setId(STUDENT_ID);
        student.setImagePath(imagePath);
        return student;
    }
}
