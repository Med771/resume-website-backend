package ru.ai.sin.logic.recruiter;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.ai.sin.logic.recruiter.dto.PatchRecruiterReq;
import ru.ai.sin.logic.recruiter.dto.RecruiterDTO;
import ru.ai.sin.models.enums.CooperationFormat;

import static org.assertj.core.api.Assertions.assertThat;

class RecruiterPatchMappingTest {

    private final RecruiterMapper mapper = Mappers.getMapper(RecruiterMapper.class);

    @Test
    void patchWritesValuesAndKeepsOtherFields() {
        RecruiterEnt entity = new RecruiterEnt();
        entity.setCompanyName("ACME");

        mapper.patchEntityFromDto(new PatchRecruiterReq(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                CooperationFormat.INTERNSHIP,
                true
        ), entity);

        assertThat(entity.getCompanyName()).isEqualTo("ACME");
        assertThat(entity.getCooperationFormat()).isEqualTo(CooperationFormat.INTERNSHIP);
        assertThat(entity.getReadyToRequestStudents()).isTrue();
    }

    @Test
    void patchNullDoesNotOverwrite() {
        RecruiterEnt entity = new RecruiterEnt();
        entity.setCooperationFormat(CooperationFormat.VACANCY);
        entity.setReadyToRequestStudents(false);

        mapper.patchEntityFromDto(new PatchRecruiterReq(
                "New",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        ), entity);

        assertThat(entity.getCompanyName()).isEqualTo("New");
        assertThat(entity.getCooperationFormat()).isEqualTo(CooperationFormat.VACANCY);
        assertThat(entity.getReadyToRequestStudents()).isFalse();
    }

    @Test
    void toDtoMapsNewFields() {
        RecruiterEnt entity = new RecruiterEnt();
        entity.setCompanyName("ACME");
        entity.setCooperationFormat(CooperationFormat.OTHER);
        entity.setReadyToRequestStudents(true);

        RecruiterDTO dto = mapper.toDTO(entity);

        assertThat(dto.cooperationFormat()).isEqualTo(CooperationFormat.OTHER);
        assertThat(dto.readyToRequestStudents()).isTrue();
    }
}
