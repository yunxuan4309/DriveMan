package com.homework.driveman.utils;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 工具类 — 签发/解析 Token
 */
@Component
public class JwtUtils {

    /** token 有效期 7 天 */
    private static final long EXPIRATION = 7 * 24 * 60 * 60 * 1000L;

    @Value("${drive.jwt.secret:DefaultSecretKeyForDriveMan2026MustBe256BitsLong!}")
    private String secret;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /** 签发 Token */
    public String generateToken(CurrentUser currentUser) {
        return Jwts.builder()
                .subject(currentUser.getUserId().toString())
                .claim("username", currentUser.getUsername())
                .claim("role", currentUser.getRole())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + EXPIRATION))
                .signWith(getKey())
                .compact();
    }

    /** 解析 Token，返回 null 表示无效 */
    public CurrentUser parseToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return new CurrentUser(
                    Integer.valueOf(claims.getSubject()),
                    claims.get("username", String.class),
                    claims.get("role", Integer.class)
            );
        } catch (JwtException e) {
            return null;
        }
    }
}
