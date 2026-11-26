package ru.ai.sin.logic.recruitment;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(name = "recruitment_request_specialities")
@IdClass(RecruitmentRequestSpecialityId.class)
@Getter
@Setter
@NoArgsConstructor
public class RecruitmentRequestSpecialityEnt {

    @Id
    @Column(name = "recruitment_request_id", nullable = false)
    private UUID recruitmentRequestId;

    @Id
    @Column(name = "speciality_id", nullable = false)
    private Long specialityId;
}
