package ru.ai.sin.models.embeddables;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import jakarta.validation.constraints.Email;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserInformation {

    @Column(name = "first_name")
    private String firstName;

    @Column(name = "last_name")
    private String lastName;

    @Column(unique = true)
    @Email(message = "Email should be valid")
    private String email;
}
