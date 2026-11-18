package ru.ai.sin.logic.city;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import ru.ai.sin.exception.models.BadRequestException;
import ru.ai.sin.logic.student.StudentEnt;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CityCatalogTest {

    @Mock
    private CityRepo cityRepo;

    private CityCatalog catalog;
    private CityEnt kazan;

    @BeforeEach
    void setUp() {
        catalog = new CityCatalog(cityRepo);
        kazan = new CityEnt();
        kazan.setId(8L);
        kazan.setName("Казань");
    }

    @Test
    void text_findsCityIgnoringCase() {
        when(cityRepo.findFirstByNameIgnoreCase("казань")).thenReturn(Optional.of(kazan));
        StudentEnt student = new StudentEnt();

        catalog.assignIfPresent(student, null, "  казань ");

        assertThat(student.getCityCatalog()).isSameAs(kazan);
        assertThat(student.getCity()).isEqualTo("Казань");
    }

    @Test
    void cityId_isUsedWhenBothAreSent() {
        when(cityRepo.findById(8L)).thenReturn(Optional.of(kazan));
        StudentEnt student = new StudentEnt();

        catalog.assignIfPresent(student, 8L, "Москва");

        assertThat(student.getCity()).isEqualTo("Казань");
    }

    @Test
    void unknownName_isBadRequest() {
        when(cityRepo.findFirstByNameIgnoreCase("Нет такого")).thenReturn(Optional.empty());
        StudentEnt student = new StudentEnt();
        student.setCity("Казань");

        assertThatThrownBy(() -> catalog.assignIfPresent(student, null, "Нет такого"))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("справочник");
        assertThat(student.getCity()).isEqualTo("Казань");
    }

    @Test
    void blankText_doesNotChangeCity() {
        StudentEnt student = new StudentEnt();
        student.setCity("Казань");
        student.setCityCatalog(kazan);

        catalog.assignIfPresent(student, null, "  ");

        assertThat(student.getCity()).isEqualTo("Казань");
        assertThat(student.getCityCatalog()).isSameAs(kazan);
    }

    @Test
    void oldSnapshotName_staysWithoutIdWhenCityIsGone() {
        when(cityRepo.findFirstByNameIgnoreCase("Старый")).thenReturn(Optional.empty());
        StudentEnt student = new StudentEnt();

        catalog.restoreFromSnapshot(student, "Старый");

        assertThat(student.getCity()).isEqualTo("Старый");
        assertThat(student.getCityCatalog()).isNull();
        assertThat(catalog.idOfName("Старый")).isNull();
    }
}
