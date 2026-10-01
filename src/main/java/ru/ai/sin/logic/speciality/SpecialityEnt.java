package ru.ai.sin.logic.speciality;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ru.ai.sin.models.embeddables.TimeStamped;

@Entity
@Table(name = "specialities")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class SpecialityEnt {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 128)
    private String name;

    @Column(name = "icon_path", length = 512)
    private String iconPath;

    @Embedded
    private TimeStamped timestamps = new TimeStamped();
}
