package com.memoire.assistant.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpirationMs;

    @Value("${spring.profiles.active:}")
    private String activeProfiles;

    @PostConstruct
    void validateConfiguration() {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException("jwt.secret est obligatoire");
        }

        boolean productionLike = activeProfiles != null
            && activeProfiles.toLowerCase().contains("prod");

        if (productionLike) {
            if ("your-secret-key-here-change-in-production".equals(jwtSecret)) {
                throw new IllegalStateException("jwt.secret ne doit pas utiliser la valeur par defaut en production");
            }

            if (jwtSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
                throw new IllegalStateException("jwt.secret doit contenir au moins 32 octets en production");
            }
        }

        if (jwtExpirationMs <= 0) {
            throw new IllegalStateException("jwt.expiration doit etre strictement positif");
        }
    }

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(String username, String role) {
        return generateToken(username, role, null, null, null);
    }

    public String generateToken(String username, String role, UUID userId, UUID recruiterId, UUID companyId) {
        return Jwts.builder()
                .setSubject(username)
                .claim("role", role)
                .claim("userId", userId != null ? userId.toString() : null)
                .claim("recruiterId", recruiterId != null ? recruiterId.toString() : null)
                .claim("companyId", companyId != null ? companyId.toString() : null)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUsernameFromToken(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
                .parseClaimsJws(token).getBody().getSubject();
    }

    public String getRoleFromToken(String token) {
        return (String) getAllClaims(token).get("role");
    }

    public UUID getUserIdFromToken(String token) {
        return parseUuidClaim(token, "userId");
    }

    public UUID getRecruiterIdFromToken(String token) {
        return parseUuidClaim(token, "recruiterId");
    }

    public UUID getCompanyIdFromToken(String token) {
        return parseUuidClaim(token, "companyId");
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(getSigningKey()).build().parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    private Claims getAllClaims(String token) {
        return Jwts.parserBuilder().setSigningKey(getSigningKey()).build()
                .parseClaimsJws(token).getBody();
    }

    private UUID parseUuidClaim(String token, String claimName) {
        Object raw = getAllClaims(token).get(claimName);
        if (raw == null) {
            return null;
        }
        try {
            return UUID.fromString(String.valueOf(raw));
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}
