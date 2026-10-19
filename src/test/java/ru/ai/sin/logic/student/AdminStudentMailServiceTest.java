package ru.ai.sin.logic.student;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.config.property.MailProperties;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.student.dto.AdminStudentMailReq;
import ru.ai.sin.logic.student.dto.AdminStudentMailResult;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.logic.verification.VerificationOtpMailer;
import ru.ai.sin.models.embeddables.UserInformation;
import ru.ai.sin.models.enums.RoleEnum;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminStudentMailServiceTest {

    @Mock
    private UserRepo userRepo;
    @Mock
    private VerificationOtpMailer verificationOtpMailer;
    @Mock
    private SecurityHelper securityHelper;

    private MailProperties mailProperties;
    private AdminStudentMailService service;

    @BeforeEach
    void setUp() {
        mailProperties = new MailProperties();
        mailProperties.setEnabled(true);
        mailProperties.setFrom("from@test.local");
        service = new AdminStudentMailService(userRepo, mailProperties, verificationOtpMailer, securityHelper);
    }

    @Test
    void send_splitsSentAndMissingEmail() {
        UUID withEmail = UUID.randomUUID();
        UUID withoutEmail = UUID.randomUUID();
        UserEnt ivan = student("ivan", withEmail, "ivan@test.local", "Иван", "Петров");
        UserEnt anna = student("anna", withoutEmail, null, "Анна", "Смирнова");
        when(userRepo.findStudentsByStudentIds(List.of(withEmail, withoutEmail))).thenReturn(List.of(ivan, anna));
        when(verificationOtpMailer.trySendPlain("ivan@test.local", "Тема", "Текст")).thenReturn(Optional.empty());
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        AdminStudentMailResult result = service.send(new AdminStudentMailReq(
                List.of(withEmail, withoutEmail), false, "Тема", "Текст"));

        assertThat(result.sent()).containsExactly(
                new AdminStudentMailResult.Sent(withEmail, "Иван Петров", "ivan@test.local"));
        assertThat(result.skipped()).containsExactly(
                new AdminStudentMailResult.Skipped(withoutEmail, "Анна Смирнова", null, "NO_EMAIL"));
        assertThat(result.failed()).isEmpty();
    }

    @Test
    void send_duplicateEmailGoesToFirstStudent() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UserEnt ivan = student("ivan", first, "same@test.local", "Иван", "Петров");
        UserEnt oleg = student("oleg", second, "same@test.local", "Олег", "Кузнецов");
        when(userRepo.findStudentsByStudentIds(List.of(first, second))).thenReturn(List.of(oleg, ivan));
        when(verificationOtpMailer.trySendPlain("same@test.local", "Тема", "Текст")).thenReturn(Optional.empty());
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        AdminStudentMailResult result = service.send(new AdminStudentMailReq(
                List.of(first, second), false, "Тема", "Текст"));

        assertThat(result.sent()).extracting(AdminStudentMailResult.Sent::studentId).containsExactly(first);
        assertThat(result.skipped()).containsExactly(
                new AdminStudentMailResult.Skipped(second, "Олег Кузнецов", "same@test.local", "DUPLICATE"));
        verify(verificationOtpMailer).trySendPlain("same@test.local", "Тема", "Текст");
    }

    @Test
    void send_unknownIdIsNotFound() {
        UUID known = UUID.randomUUID();
        UUID missing = UUID.randomUUID();
        UserEnt ivan = student("ivan", known, "ivan@test.local", "Иван", "Петров");
        when(userRepo.findStudentsByStudentIds(List.of(missing, known))).thenReturn(List.of(ivan));
        when(verificationOtpMailer.trySendPlain(anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        AdminStudentMailResult result = service.send(new AdminStudentMailReq(
                List.of(missing, known), false, "Тема", "Текст"));

        assertThat(result.skipped()).containsExactly(
                new AdminStudentMailResult.Skipped(missing, null, null, "NOT_FOUND"));
        assertThat(result.sent()).extracting(AdminStudentMailResult.Sent::studentId).containsExactly(known);
    }

    @Test
    void send_smtpErrorDoesNotStopOthers() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        UserEnt ivan = student("ivan", first, "ivan@test.local", "Иван", "Петров");
        UserEnt anna = student("anna", second, "anna@test.local", "Анна", "Смирнова");
        when(userRepo.findStudentsByStudentIds(List.of(first, second))).thenReturn(List.of(ivan, anna));
        when(verificationOtpMailer.trySendPlain("ivan@test.local", "Тема", "Текст"))
                .thenReturn(Optional.of("MessagingException: SMTP timeout"));
        when(verificationOtpMailer.trySendPlain("anna@test.local", "Тема", "Текст")).thenReturn(Optional.empty());
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        AdminStudentMailResult result = service.send(new AdminStudentMailReq(
                List.of(first, second), false, "Тема", "Текст"));

        assertThat(result.failed()).containsExactly(new AdminStudentMailResult.Failed(
                first, "Иван Петров", "ivan@test.local", "MessagingException: SMTP timeout"));
        assertThat(result.sent()).extracting(AdminStudentMailResult.Sent::studentId).containsExactly(second);
    }

    @Test
    void send_usesCardEmailWhenRegistrationEmailMissing() {
        UUID id = UUID.randomUUID();
        UserEnt ivan = student("ivan", id, null, "Иван", "Петров");
        ivan.getStudent().getUserInformation().setEmail("card@test.local");
        when(userRepo.findStudentsByStudentIds(List.of(id))).thenReturn(List.of(ivan));
        when(verificationOtpMailer.trySendPlain("card@test.local", "Тема", "Текст")).thenReturn(Optional.empty());
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        AdminStudentMailResult result = service.send(new AdminStudentMailReq(List.of(id), false, "Тема", "Текст"));

        assertThat(result.sent()).containsExactly(
                new AdminStudentMailResult.Sent(id, "Иван Петров", "card@test.local"));
    }

    @Test
    void send_withoutSelection_throwsBeforeSend() {
        assertThatThrownBy(() -> service.send(new AdminStudentMailReq(List.of(), false, "Тема", "Текст")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("studentIds");
        verify(verificationOtpMailer, never()).trySendPlain(anyString(), anyString(), anyString());
        verify(userRepo, never()).findAllStudentUsers();
    }

    @Test
    void send_moreThan200_throwsBeforeSend() {
        List<UUID> ids = new ArrayList<>();
        for (int i = 0; i < 201; i++) {
            ids.add(UUID.randomUUID());
        }

        assertThatThrownBy(() -> service.send(new AdminStudentMailReq(ids, false, "Тема", "Текст")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("200");
        verify(verificationOtpMailer, never()).trySendPlain(anyString(), anyString(), anyString());
        verify(userRepo, never()).findStudentsByStudentIds(org.mockito.ArgumentMatchers.anyCollection());
    }

    @Test
    void send_mailDisabled_throwsBeforeSend() {
        mailProperties.setEnabled(false);
        UUID id = UUID.randomUUID();
        when(userRepo.findStudentsByStudentIds(List.of(id)))
                .thenReturn(List.of(student("ivan", id, "ivan@test.local", "Иван", "Петров")));

        assertThatThrownBy(() -> service.send(new AdminStudentMailReq(List.of(id), false, "Тема", "Текст")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("выключена");
        verify(verificationOtpMailer, never()).trySendPlain(anyString(), anyString(), anyString());
    }

    private static UserEnt student(String username, UUID studentId, String email, String firstName, String lastName) {
        StudentEnt student = new StudentEnt();
        student.setId(studentId);
        UserInformation info = new UserInformation();
        info.setFirstName(firstName);
        info.setLastName(lastName);
        student.setUserInformation(info);
        UserEnt user = new UserEnt(RoleEnum.STUDENT, username, "hash");
        user.setStudent(student);
        user.setRegistrationEmail(email);
        return user;
    }
}
