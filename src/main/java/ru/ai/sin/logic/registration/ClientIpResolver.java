package ru.ai.sin.logic.registration;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

/**
 * IP для лимитов. При работе за доверенным reverse-proxy нужен корректный {@code ForwardedHeaderFilter}
 * / {@code server.forward-headers-strategy}, иначе {@code X-Forwarded-For} от клиента нельзя доверять.
 */
public final class ClientIpResolver {

    private ClientIpResolver() {}

    public static String resolve(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(forwarded)) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
