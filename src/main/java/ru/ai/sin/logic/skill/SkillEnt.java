package ru.ai.sin.logic.skill;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ru.ai.sin.models.embeddables.TimeStamped;

@Entity
@Table(name = "skills")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class SkillEnt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, length = 128)
    private String name;

    @Embedded
    private TimeStamped timestamps = new TimeStamped();

    public SkillEnt(String name) {
        this.name = name;
    }
}
