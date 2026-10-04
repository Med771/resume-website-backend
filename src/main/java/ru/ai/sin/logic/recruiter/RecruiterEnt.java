package ru.ai.sin.logic.recruiter;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.hibernate.annotations.UuidGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ru.ai.sin.models.embeddables.ContactInformation;
import ru.ai.sin.models.embeddables.TimeStamped;
import ru.ai.sin.models.embeddables.UserInformation;

import java.util.UUID;

@Entity
@Table(name = "recruiters")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class RecruiterEnt {
    @Id
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    private UUID id;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "city")
    private String city;

    @Embedded
    private UserInformation userInformation = new UserInformation();

    @Embedded
    private TimeStamped timestamps = new TimeStamped();

    @Embedded
    private ContactInformation contactInformation = new ContactInformation();
}
