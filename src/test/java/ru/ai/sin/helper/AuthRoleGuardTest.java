package ru.ai.sin.helper;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import ru.ai.sin.exception.models.ForbiddenException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AuthRoleGuardTest {

    private final AuthRoleGuard guard = new AuthRoleGuard();

    @Test
    void requireAdmin_allowsAdmin() {
        UserDetails admin = User.withUsername("admin").password("x").roles("ADMIN").build();
        assertThatCode(() -> guard.requireAdmin(admin)).doesNotThrowAnyException();
    }

    @Test
    void requireAdmin_rejectsStudent() {
        UserDetails student = User.withUsername("student").password("x").roles("STUDENT").build();
        assertThatThrownBy(() -> guard.requireAdmin(student)).isInstanceOf(ForbiddenException.class);
    }

    @Test
    void requireFrontendUser_allowsStudent() {
        UserDetails student = User.withUsername("student").password("x").roles("STUDENT").build();
        assertThatCode(() -> guard.requireFrontendUser(student)).doesNotThrowAnyException();
    }

    @Test
    void requireFrontendUser_rejectsAdmin() {
        UserDetails admin = User.withUsername("admin").password("x").roles("ADMIN").build();
        assertThatThrownBy(() -> guard.requireFrontendUser(admin)).isInstanceOf(ForbiddenException.class);
    }
}
