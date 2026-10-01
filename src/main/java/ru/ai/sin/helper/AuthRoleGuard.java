package ru.ai.sin.helper;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import ru.ai.sin.exception.models.ForbiddenException;
import ru.ai.sin.models.enums.RoleEnum;

@Component
public class AuthRoleGuard {

    private static final String ADMIN_ROLE = "ROLE_" + RoleEnum.ADMIN.getRole();

    public void requireAdmin(UserDetails userDetails) {
        if (userDetails == null || !hasAuthority(userDetails, ADMIN_ROLE)) {
            throw new ForbiddenException("Доступ к админ-панели разрешён только администраторам");
        }
    }

    public void requireFrontendUser(UserDetails userDetails) {
        if (userDetails != null && hasAuthority(userDetails, ADMIN_ROLE)) {
            throw new ForbiddenException("Для входа администратора используйте /auth/admin/login");
        }
    }

    private static boolean hasAuthority(UserDetails userDetails, String role) {
        if (userDetails.getAuthorities() == null) {
            return false;
        }
        for (GrantedAuthority authority : userDetails.getAuthorities()) {
            if (role.equals(authority.getAuthority())) {
                return true;
            }
        }
        return false;
    }
}
