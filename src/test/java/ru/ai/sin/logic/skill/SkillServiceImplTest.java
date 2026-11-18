package ru.ai.sin.logic.skill;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.helper.SecurityHelper;
import ru.ai.sin.logic.skill.dto.AddSkillReq;
import ru.ai.sin.logic.skill.dto.FilterSkillReq;
import ru.ai.sin.logic.skill.dto.SkillDTO;
import ru.ai.sin.logic.skill.dto.UpdateSkillReq;
import ru.ai.sin.logic.speciality.SpecialityEnt;
import ru.ai.sin.models.PageResponse;
import ru.ai.sin.tools.SkillTools;
import ru.ai.sin.tools.SpecialityTools;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SkillServiceImplTest {

    @Mock
    private SkillRepo skillRepo;
    @Mock
    private SkillTools skillTools;
    @Mock
    private SpecialityTools specialityTools;
    @Mock
    private SecurityHelper securityHelper;

    private SkillServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new SkillServiceImpl(
                skillRepo,
                Mappers.getMapper(SkillMapper.class),
                skillTools,
                specialityTools,
                securityHelper);
    }

    @Test
    void create_nameOnly_isRootWithoutSpeciality() {
        when(skillRepo.save(any())).thenAnswer(invocation -> {
            SkillEnt saved = invocation.getArgument(0);
            saved.setId(1L);
            return saved;
        });

        SkillDTO dto = service.create(new AddSkillReq("Java", null, null));

        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.name()).isEqualTo("Java");
        assertThat(dto.parentId()).isNull();
        assertThat(dto.specialityId()).isNull();
    }

    @Test
    void update_nameOnly_keepsParentAndSpeciality() {
        SpecialityEnt speciality = speciality(9L);
        SkillEnt skill = skill(1L, "Java");
        skill.setSpeciality(speciality);
        when(skillTools.getSkillOrThrow(1L)).thenReturn(skill);

        SkillDTO dto = service.updateById(1L, new UpdateSkillReq("Java SE", null, null, null, null));

        assertThat(dto.name()).isEqualTo("Java SE");
        assertThat(dto.parentId()).isNull();
        assertThat(dto.specialityId()).isEqualTo(9L);
        assertThat(skill.getSpeciality()).isSameAs(speciality);
    }

    @Test
    void update_parentOfDescendant_rejected() {
        SkillEnt root = skill(1L, "Java");
        SkillEnt child = skill(2L, "Spring");
        child.setParent(root);
        when(skillTools.getSkillOrThrow(1L)).thenReturn(root);
        when(skillTools.getSkillOrThrow(2L)).thenReturn(child);

        assertThatThrownBy(() -> service.updateById(1L, new UpdateSkillReq("Java", 2L, null, null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("замыкает");
        assertThat(root.getParent()).isNull();
    }

    @Test
    void update_specialityOnChild_rejected() {
        SkillEnt root = skill(1L, "Java");
        SkillEnt child = skill(2L, "Spring");
        child.setParent(root);
        when(skillTools.getSkillOrThrow(2L)).thenReturn(child);

        assertThatThrownBy(() -> service.updateById(2L, new UpdateSkillReq("Spring", null, 7L, null, null)))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("только корню");
        assertThat(child.getSpeciality()).isNull();
    }

    @Test
    void filter_rootHasSpeciality_childDoesNot() {
        SpecialityEnt speciality = speciality(4L);
        SkillEnt root = skill(1L, "Java");
        root.setSpeciality(speciality);
        SkillEnt child = skill(2L, "Spring");
        child.setParent(root);
        when(skillRepo.findAll(ArgumentMatchers.<Specification<SkillEnt>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(root, child)));

        PageResponse<SkillDTO> page = service.getAllByFilter(PageRequest.of(0, 20), new FilterSkillReq(null));

        assertThat(page.data().get(0).specialityId()).isEqualTo(4L);
        assertThat(page.data().get(0).parentId()).isNull();
        assertThat(page.data().get(1).parentId()).isEqualTo(1L);
        assertThat(page.data().get(1).specialityId()).isNull();
    }

    private static SkillEnt skill(long id, String name) {
        SkillEnt skill = new SkillEnt(name);
        skill.setId(id);
        return skill;
    }

    private static SpecialityEnt speciality(long id) {
        SpecialityEnt speciality = new SpecialityEnt();
        speciality.setId(id);
        return speciality;
    }
}
