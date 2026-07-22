package com.medibridge.common.security;

import com.medibridge.common.enums.UserType;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.UUID;

/**
 * HS256 access tokens. Claims carry everything needed to rebuild the principal,
 * so authenticated requests need no database round-trip.
 *
 * <p>Tradeoff worth knowing: because nothing is checked against the DB per
 * request, suspending an account does not take effect until the current access
 * token expires (15 minutes). Refresh tokens are revocable immediately, which
 * caps the exposure.
 */
@Service
public class JwtService {

    private static final Logger log = LoggerFactory.getLogger(JwtService.class);

    private static final String CLAIM_TYPE = "type";
    private static final String CLAIM_EMAIL = "email";
    private static final String CLAIM_NAME = "name";

    private final SecretKey key;
    private final long accessTokenMinutes;
    private final long refreshTokenDays;

    public JwtService(
            @Value("${medibridge.jwt.secret}") String secret,
            @Value("${medibridge.jwt.access-token-minutes}") long accessTokenMinutes,
            @Value("${medibridge.jwt.refresh-token-days}") long refreshTokenDays) {

        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (keyBytes.length < 32) {
            throw new IllegalStateException(
                    "medibridge.jwt.secret must be at least 32 bytes for HS256. "
                            + "Set a longer value in application-local.yml.");
        }
        this.key = Keys.hmacShaKeyFor(keyBytes);
        this.accessTokenMinutes = accessTokenMinutes;
        this.refreshTokenDays = refreshTokenDays;
    }

    public String generateAccessToken(String userId, String email, String fullName, UserType type) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId)
                .claim(CLAIM_TYPE, type.name())
                .claim(CLAIM_EMAIL, email)
                .claim(CLAIM_NAME, fullName)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTokenMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    /** Opaque random string - refresh tokens are looked up, not parsed. */
    public String generateRefreshToken() {
        byte[] random = new byte[48];
        new java.security.SecureRandom().nextBytes(random);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(random)
                + UUID.randomUUID().toString().replace("-", "");
    }

    public String hashRefreshToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    public long getRefreshTokenDays() {
        return refreshTokenDays;
    }

    /** @return the principal, or null when the token is absent/invalid/expired. */
    public SecurityUser parse(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new SecurityUser(
                    claims.getSubject(),
                    claims.get(CLAIM_EMAIL, String.class),
                    claims.get(CLAIM_NAME, String.class),
                    UserType.valueOf(claims.get(CLAIM_TYPE, String.class)));

        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Rejected JWT: {}", e.getMessage());
            return null;
        }
    }
}
