package ru.ai.sin.models.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import jakarta.validation.constraints.Pattern;

import lombok.*;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContactInformation {

    @Column(name = "phone_number", length = 16)
    @Pattern(regexp = "\\+?\\d{1,32}", message = "Phone number must contain 1-32 digits and optional + at start")
    private String phoneNumber;

    @Column(name = "telegram_username", length = 32)
    private String telegramUsername;

    @Column(name = "telegram_user_id", unique = true, length = 16)
    private String telegramUserId;
}