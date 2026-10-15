package ru.ai.sin.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import ru.ai.sin.logic.student.StudentEnt;
import ru.ai.sin.logic.student.StudentRepo;
import ru.ai.sin.logic.student.StudentSpecifications;
import ru.ai.sin.logic.student.dto.FilterStudentReq;
import ru.ai.sin.models.embeddables.UserInformation;
import ru.ai.sin.models.enums.BusynessEnum;
import ru.ai.sin.models.enums.CourseEnum;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Поиск по {@code findString} для {@code POST /student/filter} и {@code POST /student/cardsFilter}:
 * оба списка используют {@link StudentSpecifications#byFilters}.
 */
class StudentFindStringIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private StudentRepo studentRepo;

    @DynamicPropertySource
    static void testProperties(DynamicPropertyRegistry registry) {
        registry.add("MAIL_HOST", () -> "localhost");
        registry.add("MAIL_PORT", () -> "1025");
        registry.add("MAIL_USERNAME", () -> "test@localhost");
        registry.add("MAIL_PASSWORD", () -> "test");
        registry.add("MAIL_FROM", () -> "test@localhost");
        registry.add("TELEGRAM_BOT_TOKEN", () -> "test-bot-token");
        registry.add("TELEGRAM_BOT_USERNAME", () -> "test_bot");
        registry.add("TELEGRAM_WEBHOOK_SECRET", () -> "test-webhook-secret");
        registry.add("ADMIN_USERNAME", () -> "admin");
        registry.add("ADMIN_PASSWORD", () -> "admin-local-test");
        registry.add("JWT_ACCESS_SECRET", () -> "bG9jYWwtdGVzdC1hY2Nlc3Mtc2VjcmV0LWtleS0zMmIh");
        registry.add("JWT_REFRESH_SECRET", () -> "bG9jYWwtdGVzdC1yZWZyZXNoLXNlY3JldC1rZXktMzJiIQ==");
        registry.add("JWT_COOKIE_SECURE", () -> "false");
        registry.add("JWT_COOKIE_SAME_SITE", () -> "Lax");
        registry.add("SWAGGER_VERSION", () -> "1.0.0");
        registry.add("SWAGGER_ON", () -> "false");
    }

    @Test
    @Transactional
    void findsSubstringInLastName() {
        StudentEnt student = save("Анна", "Моисеева", "Сергеевна", "текст", true);

        assertThat(search("мои", false, null)).contains(student.getId());
    }

    @Test
    @Transactional
    void findsSubstringInFirstName() {
        StudentEnt student = save("Моисей", "Петров", "Иванович", "текст", true);

        assertThat(search("моис", false, null)).contains(student.getId());
    }

    @Test
    @Transactional
    void findsSubstringInMiddleName() {
        StudentEnt student = save("Иван", "Петров", "Моисеевич", "текст", true);

        assertThat(search("моисе", false, null)).contains(student.getId());
    }

    @Test
    @Transactional
    void findsSubstringInBioWhenNameDoesNotMatch() {
        StudentEnt student = save("Анна", "Петрова", "Сергеевна", "мои проекты", true);

        assertThat(search("мои", false, null)).contains(student.getId());
    }

    @Test
    @Transactional
    void searchIsCaseInsensitive() {
        StudentEnt student = save("Анна", "Моисеева", "Сергеевна", "текст", true);

        assertThat(search("МОИ", false, null)).contains(student.getId());
        assertThat(search("мои", false, null)).contains(student.getId());
    }

    @Test
    @Transactional
    void findsByLastNameWhenMiddleNameIsNull() {
        StudentEnt student = save("Иван", "Иванов", null, "текст", true);

        assertThat(search("иван", false, null)).contains(student.getId());
    }

    @Test
    @Transactional
    void shortQueryIsNotRejected() {
        StudentEnt student = save("Анна", "Моисеева", null, "текст", true);

        assertThat(search("мои", false, null)).contains(student.getId());
    }

    @Test
    @Transactional
    void shortQueryDoesNotMatchUnrelatedName() {
        StudentEnt student = save("Диана", "Мурлаева", null, null, true);

        assertThat(search("мои", false, null)).doesNotContain(student.getId());
    }

    @Test
    @Transactional
    void fuzzyMatchAllowsSmallTypo() {
        StudentEnt student = save("Анна", "Моисеева", null, "текст", true);

        assertThat(search("моисеев", false, null)).contains(student.getId());
    }

    @Test
    @Transactional
    void unknownTextReturnsEmptyPage() {
        save("Анна", "Петрова", "Сергеевна", "обычный текст", true);

        assertThat(search("zzzznotaname", false, null)).isEmpty();
    }

    @Test
    @Transactional
    void blankFindStringDoesNotFilterByText() {
        StudentEnt student = save("Анна", "Петрова", null, "текст", true);

        assertThat(search("   ", false, null)).contains(student.getId());
        assertThat(search(null, false, null)).contains(student.getId());
    }

    @Test
    @Transactional
    void hiddenStudentIsVisibleOnlyWhenCatalogIncludesHidden() {
        StudentEnt hidden = save("Анна", "Моисеева", null, "текст", false);

        assertThat(search("мои", false, null)).doesNotContain(hidden.getId());
        assertThat(search("мои", true, null)).contains(hidden.getId());
    }

    @Test
    @Transactional
    void findStringCombinedWithOtherCourseExcludesStudent() {
        StudentEnt student = save("Анна", "Моисеева", null, "текст", true);

        assertThat(search("мои", false, Set.of(CourseEnum.SECOND))).doesNotContain(student.getId());
    }

    @Test
    @Transactional
    void findStringCombinedWithSameCourseKeepsStudent() {
        StudentEnt student = save("Анна", "Моисеева", null, "текст", true);

        assertThat(search("мои", false, Set.of(CourseEnum.FIRST))).contains(student.getId());
    }

    private StudentEnt save(
            String firstName,
            String lastName,
            String middleName,
            String bio,
            boolean catalogVisible
    ) {
        StudentEnt student = new StudentEnt();
        student.setBirthDate(LocalDate.of(2002, 3, 4));
        student.setCourse(CourseEnum.FIRST);
        student.setBusyness(BusynessEnum.FREE);
        student.setUserInformation(new UserInformation(
                firstName,
                lastName,
                UUID.randomUUID() + "@find.test"));
        student.setMiddleName(middleName);
        student.setBio(bio);
        student.setCatalogVisible(catalogVisible);
        return studentRepo.saveAndFlush(student);
    }

    private List<UUID> search(String findString, boolean includeHidden, Set<CourseEnum> course) {
        FilterStudentReq filter = new FilterStudentReq(
                findString,
                course,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        return studentRepo.findAll(
                        StudentSpecifications.byFilters(filter, includeHidden, false),
                        PageRequest.of(0, 200))
                .map(StudentEnt::getId)
                .toList();
    }
}
