package ru.ai.sin.logic.registration;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.ai.sin.config.property.RegistrationProperties;
import ru.ai.sin.exception.models.BadRequestException;

@Component
@RequiredArgsConstructor
public class RegistrationPasswordPolicy {

    private final RegistrationProperties registrationProperties;

    public void validate(String password) {
        if (password == null) {
            throw new BadRequestException("Укажите пароль");
        }
        if (password.length() > 72) {
            throw new BadRequestException("Пароль слишком длинный");
        }
        int min = Math.max(8, registrationProperties.getMinPasswordLength());
        if (password.length() < min) {
            throw new BadRequestException("Пароль должен быть не короче " + min + " символов");
        }
        if (registrationProperties.isRequireLetterAndDigit()) {
            boolean letter = password.chars().anyMatch(Character::isLetter);
            boolean digit = password.chars().anyMatch(Character::isDigit);
            if (!letter || !digit) {
                throw new BadRequestException("Пароль должен содержать букву и цифру");
            }
        }
    }
}
