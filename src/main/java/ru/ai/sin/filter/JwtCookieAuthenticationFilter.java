package ru.ai.sin.filter;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ru.ai.sin.exception.models.NotFoundException;
import ru.ai.sin.helper.CookieHelper;
import ru.ai.sin.helper.JwtHelper;
import ru.ai.sin.config.property.JwtProperties;

import java.util.Arrays;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private final JwtHelper jwtHelper;
    private final CookieHelper cookieHelper;
    private final JwtProperties jwtProperties;
    private final UserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) {

        try {
            String accessToken = extractCookie(request, jwtProperties.getCookie().getAccessTokenName());
            String refreshToken = extractCookie(request, jwtProperties.getCookie().getRefreshTokenName());

            if (accessToken != null && !accessToken.isBlank()) {
                try {
                    String username = jwtHelper.getUsernameFromAccessToken(accessToken);
                    trySetAuthentication(username, request);
                } catch (ExpiredJwtException e) {
                    log.debug("Access token expired: {}", e.getMessage());
                    clearAuthCookies(response);
                } catch (JwtException e) {
                    log.warn("Invalid access JWT: {}", e.getMessage());
                    clearAuthCookies(response);
                } catch (UserNotFoundInTokenException e) {
                    log.warn("Access token for unknown user {}, clearing cookies", e.username);
                    clearAuthCookies(response);
                }
                filterChain.doFilter(request, response);
                return;
            }

            if (refreshToken != null && !refreshToken.isBlank()) {
                try {
                    String username = jwtHelper.getUsernameFromRefreshToken(refreshToken);
                    if (username != null) {
                        UserDetails userDetails = loadUserOrThrow(username);
                        String newAccessToken = jwtHelper.generateAccessToken(userDetails.getUsername());

                        ResponseCookie newAccessCookie = cookieHelper.createAccessTokenCookie(newAccessToken);
                        response.addHeader(HttpHeaders.SET_COOKIE, newAccessCookie.toString());

                        setAuthentication(userDetails, request);
                    }
                } catch (ExpiredJwtException e) {
                    log.debug("Refresh token expired: {}", e.getMessage());
                    clearAuthCookies(response);
                } catch (JwtException e) {
                    log.warn("Invalid refresh JWT: {}", e.getMessage());
                    clearAuthCookies(response);
                } catch (UserNotFoundInTokenException e) {
                    log.warn("Refresh token for unknown user {}, clearing cookies", e.username);
                    clearAuthCookies(response);
                }
            }

            filterChain.doFilter(request, response);
        } catch (Exception e) {
            log.error("Authentication error", e);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    private void trySetAuthentication(String username, HttpServletRequest request) {
        if (username == null || username.isBlank()
                || SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }
        UserDetails userDetails = loadUserOrThrow(username);
        setAuthentication(userDetails, request);
    }

    private UserDetails loadUserOrThrow(String username) {
        try {
            return userDetailsService.loadUserByUsername(username);
        } catch (NotFoundException | UsernameNotFoundException e) {
            throw new UserNotFoundInTokenException(username);
        }
    }

    private void setAuthentication(UserDetails userDetails, HttpServletRequest request) {
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            return;
        }
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.getAuthorities());
        auth.setDetails(new org.springframework.security.web.authentication.WebAuthenticationDetailsSource()
                .buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void clearAuthCookies(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookieHelper.clearAccessTokenCookie().toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieHelper.clearRefreshTokenCookie().toString());
    }

    private String extractCookie(HttpServletRequest request, String cookieName) {
        if (request.getCookies() == null) {
            return null;
        }
        return Arrays.stream(request.getCookies())
                .filter(c -> cookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    private static final class UserNotFoundInTokenException extends RuntimeException {
        private final String username;

        private UserNotFoundInTokenException(String username) {
            super("User not found: " + username);
            this.username = username;
        }
    }
}
