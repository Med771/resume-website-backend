package ru.ai.sin.logic.recruitment;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class RecruitmentRequestSpecialityId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    private UUID recruitmentRequestId;
    private Long specialityId;
}
