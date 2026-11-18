package ru.ai.sin.logic.account;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.config.property.MailProperties;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.account.dto.AdminRecruiterMailReq;
import ru.ai.sin.logic.account.dto.AdminRecruiterMailResult;
import ru.ai.sin.logic.recruiter.RecruiterEnt;
import ru.ai.sin.logic.user.UserEnt;
import ru.ai.sin.logic.user.UserRepo;
import ru.ai.sin.logic.verification.VerificationOtpMailer;
import ru.ai.sin.models.embeddables.UserInformation;
import ru.ai.sin.models.enums.RoleEnum;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminRecruiterMailServiceTest {

    @Mock
    private UserRepo userRepo;
    @Mock
    private VerificationOtpMailer verificationOtpMailer;
    @Mock
    private SecurityHelper securityHelper;

    private AdminRecruiterMailService service;

    @BeforeEach
    void setUp() {
        MailProperties mailProperties = new MailProperties();
        mailProperties.setEnabled(true);
        mailProperties.setFrom("from@test.local");
        service = new AdminRecruiterMailService(userRepo, mailProperties, verificationOtpMailer, securityHelper);
    }

    @Test
    void send_oneRecruiter_substitutesCompany() {
        UUID id = UUID.randomUUID();
        UserEnt user = recruiter("boss", id, "boss@test.local", "Иван", "Петров", "Ромашка");
        when(userRepo.findRecruitersByRecruiterIds(List.of(id))).thenReturn(List.of(user));
        when(verificationOtpMailer.trySendPlain(
                "boss@test.local",
                "Здравствуйте, Иван",
                "Ромашка, логин boss"))
                .thenReturn(Optional.empty());
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        AdminRecruiterMailResult result = service.send(new AdminRecruiterMailReq(
                List.of(id),
                false,
                "Здравствуйте, {{firstName}}",
                "{{companyName}}, логин {{username}}"));

        assertThat(result.sent()).containsExactly(
                new AdminRecruiterMailResult.Sent(id, "Иван Петров (Ромашка)", "boss@test.local"));
        assertThat(result.skipped()).isEmpty();
        assertThat(result.failed()).isEmpty();
    }

    @Test
    void send_all_skipsMissingEmail() {
        UUID withEmail = UUID.randomUUID();
        UUID withoutEmail = UUID.randomUUID();
        when(userRepo.findAllRecruiterUsers()).thenReturn(List.of(
                recruiter("boss", withEmail, "boss@test.local", "Иван", "Петров", "Ромашка"),
                recruiter("empty", withoutEmail, null, "Анна", "Смирнова", "Бета")));
        when(verificationOtpMailer.trySendPlain("boss@test.local", "Тема", "Текст")).thenReturn(Optional.empty());
        when(securityHelper.getCurrentUsername()).thenReturn("admin");

        AdminRecruiterMailResult result = service.send(new AdminRecruiterMailReq(List.of(), true, "Тема", "Текст"));

        assertThat(result.sent()).hasSize(1);
        assertThat(result.skipped()).containsExactly(
                new AdminRecruiterMailResult.Skipped(withoutEmail, "Анна Смирнова (Бета)", null, "NO_EMAIL"));
        verify(userRepo).findAllRecruiterUsers();
    }

    @Test
    void send_withoutRecipients_isBadRequest() {
        assertThatThrownBy(() -> service.send(new AdminRecruiterMailReq(List.of(), false, "Тема", "Текст")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("recruiterIds");
    }

    private static UserEnt recruiter(
            String username,
            UUID recruiterId,
            String email,
            String firstName,
            String lastName,
            String company
    ) {
        RecruiterEnt recruiter = new RecruiterEnt();
        recruiter.setId(recruiterId);
        recruiter.setCompanyName(company);
        UserInformation info = new UserInformation();
        info.setFirstName(firstName);
        info.setLastName(lastName);
        info.setEmail(email);
        recruiter.setUserInformation(info);
        UserEnt user = new UserEnt(RoleEnum.RECRUITER, lastName + " " + firstName, username, "hash");
        user.setRecruiter(recruiter);
        if (email != null) {
            user.setRegistrationEmail(email);
        }
        return user;
    }
}
