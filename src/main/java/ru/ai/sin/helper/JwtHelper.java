package ru.ai.sin.helper;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ru.ai.sin.config.property.JwtProperties;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtHelper {
    private final JwtProperties jwtProperties;

    private SecretKey accessKey;
    private SecretKey refreshKey;

    @PostConstruct
    public void init() {

        byte[] accessBytes = Decoders.BASE64.decode(jwtProperties.getAccessSecret());
        byte[] refreshBytes = Decoders.BASE64.decode(jwtProperties.getRefreshSecret());
        this.accessKey = Keys.hmacShaKeyFor(accessBytes);
        this.refreshKey = Keys.hmacShaKeyFor(refreshBytes);
    }

    public String generateAccessToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtProperties.getAccessTokenTtl())))
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .signWith(accessKey)
                .compact();
    }

    public String generateRefreshToken(String username) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(jwtProperties.getRefreshTokenTtl())))
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .id(UUID.randomUUID().toString())
                .signWith(refreshKey)
                .compact();
    }

    public String getUsernameFromToken(String token, SecretKey key) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(jwtProperties.getIssuer())
                .requireAudience(jwtProperties.getAudience())
                .clockSkewSeconds(60)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public String getUsernameFromAccessToken(String token) {
        return getUsernameFromToken(token, accessKey);
    }

    public String getUsernameFromRefreshToken(String token) {
        return getUsernameFromToken(token, refreshKey);
    }
}
