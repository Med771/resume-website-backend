package ru.ai.sin.config.property;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Data
@Component
@ConfigurationProperties(prefix = "app.registration")
public class RegistrationProperties {

    /**
     * Максимум попыток регистрации с одного IP за скользящее окно (час). 0 — без лимита.
     */
    private int rateLimitPerIpPerHour = 10;

    /**
     * Лимит заявок на регистрацию работодателя с одного IP за час (отдельно от студентов).
     */
    private int rateLimitRecruiterPerIpPerHour = 5;

    /** Неверные попытки confirm-email с одного user за час. */
    private int emailConfirmMaxAttemptsPerHour = 8;

    /** Повторная отправка OTP на почту с одного user за час. */
    private int emailResendMaxPerHour = 5;

    /**
     * Максимум навыков в одной анкете при саморегистрации.
     */
    private int maxSkillsPerProfile = 30;

    /**
     * Минимальная длина пароля (BCrypt ограничивает ~72 байта — не поднимать выше разумного).
     */
    private int minPasswordLength = 12;

    /**
     * Требовать хотя бы одну латинскую букву и одну цифру.
     */
    private boolean requireLetterAndDigit = true;

    /**
     * Максимальный размер страницы справочников на публичных эндпоинтах формы регистрации.
     */
    private int maxCatalogPageSize = 50;

    /**
     * Максимум записей опыта работы в одной саморегистрации.
     */
    private int maxExperiencesInRegistration = 12;

    /**
     * Максимум записей об образовании в одной саморегистрации.
     */
    private int maxInstitutionsInRegistration = 8;

    /**
     * Дополнительные зарезервированные логины (нижний регистр), кроме уже заведённых в {@code app.user.logins}.
     */
    private List<String> extraReservedUsernames = defaultReserved();

    private static List<String> defaultReserved() {
        List<String> r = new ArrayList<>();
        r.add("root");
        r.add("system");
        r.add("support");
        r.add("noreply");
        return r;
    }

    public boolean isReservedUsername(String username) {
        if (username == null) {
            return false;
        }
        String n = username.toLowerCase(Locale.ROOT).trim();
        for (String x : extraReservedUsernames) {
            if (n.equals(x.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    public boolean isReservedUsername(String username, UserProperties userProperties) {
        if (isReservedUsername(username)) {
            return true;
        }
        if (userProperties == null || userProperties.getLogins() == null) {
            return false;
        }
        for (UserProperties.Login login : userProperties.getLogins()) {
            if (login.getUsername() != null && login.getUsername().equalsIgnoreCase(username)) {
                return true;
            }
        }
        return false;
    }
}
