package ru.ai.sin.logic.company;

import jakarta.persistence.*;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import ru.ai.sin.logic.experience.ExperienceEnt;
import ru.ai.sin.models.embeddables.TimeStamped;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "companies")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
public class CompanyEnt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Embedded
    private TimeStamped timestamps = new TimeStamped();

    @OneToMany(mappedBy = "company", fetch = FetchType.LAZY)
    private Set<ExperienceEnt> experiences = new HashSet<>();

    public CompanyEnt(String name) {
        this.name = name;
    }
}
