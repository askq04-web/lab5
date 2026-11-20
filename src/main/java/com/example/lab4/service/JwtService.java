package com.example.lab4.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey key;
    private final long accessSeconds;
    private final long refreshSeconds;

    public JwtService(@Value("${jwt.secret}") String secret,
                      @Value("${jwt.access-seconds}") long accessSeconds,
                      @Value("${jwt.refresh-seconds}") long refreshSeconds) {
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        if (accessSeconds <= 0 || refreshSeconds <= 0)
            throw new IllegalArgumentException("Сроки токенов должны быть положительными");
        this.accessSeconds = accessSeconds;
        this.refreshSeconds = refreshSeconds;
    }

    public String createAccess(String username) { return create(username, "access", accessSeconds); }
    public String createRefresh(String username) { return create(username, "refresh", refreshSeconds); }
    public Claims validateAccess(String token) { return validate(token, "access"); }
    public Claims validateRefresh(String token) { return validate(token, "refresh"); }

    private String create(String username, String type, long seconds) {
        Instant now = Instant.ofEpochSecond(Instant.now().getEpochSecond());
        return Jwts.builder().subject(username).id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now)).expiration(Date.from(now.plusSeconds(seconds)))
                .claim("type", type).signWith(key, Jwts.SIG.HS256).compact();
    }

    private Claims validate(String token, String type) {
        Jws<Claims> jwt = Jwts.parser().verifyWith(key).require("type", type)
                .build().parseSignedClaims(token);
        Claims c = jwt.getPayload();
        if (!"HS256".equals(jwt.getHeader().getAlgorithm())
                || c.getSubject() == null || c.getSubject().isBlank()
                || c.getId() == null || c.getId().isBlank() || c.getIssuedAt() == null
                || c.getExpiration() == null || !c.getExpiration().toInstant().isAfter(Instant.now()))
            throw new JwtException("Неверные claims JWT");
        return c;
    }
}
