package com.bookmyshow.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * ============================================================
 * JwtUtil – JWT Token Utility
 * ============================================================
 *
 * Handles creation, validation, and parsing of JWT tokens.
 * Uses JJWT library with HMAC-SHA256 (HS256) signing.
 *
 * DESIGN PATTERN: Singleton (Spring @Component) – one instance
 *   shared across the application; thread-safe due to immutable state.
 */
@Slf4j
@Component  // Spring singleton bean
public class JwtUtil {

    /**
     * Secret key loaded from application.yml (jwt.secret).
     * In production: store in AWS Secrets Manager or HashiCorp Vault,
     * never hard-code in source code or config files.
     */
    @Value("${jwt.secret}")
    private String jwtSecret;

    /**
     * Token expiry in milliseconds (default: 86400000 = 24 hours).
     * Loaded from application.yml (jwt.expiration-ms).
     */
    @Value("${jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Builds the cryptographic signing key from the Base64-encoded secret.
     * Keys.hmacShaKeyFor() ensures the key length meets HS256 requirements (≥256 bits).
     *
     * @return SecretKey for HMAC-SHA256 signing/verification
     */
    private SecretKey getSigningKey() {
        // Decode Base64 secret to raw bytes, then create HMAC key
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);  // Validates key length for HS256
    }

    /**
     * Generate a signed JWT token for an authenticated user.
     *
     * Token structure:
     *   Header: { "alg": "HS256", "typ": "JWT" }
     *   Payload: { "sub": "user@email.com", "iat": ..., "exp": ... }
     *   Signature: HMAC-SHA256(base64Header + "." + base64Payload, secret)
     *
     * @param email User's email (used as 'subject' claim)
     * @return Signed JWT token string
     */
    public String generateToken(String email) {
        Date now    = new Date();                                     // Current timestamp
        Date expiry = new Date(now.getTime() + jwtExpirationMs);      // Expiry = now + 24h

        return Jwts.builder()
                .subject(email)          // 'sub' claim: user identifier
                .issuedAt(now)           // 'iat' claim: token issue time
                .expiration(expiry)      // 'exp' claim: token expiry time
                .signWith(getSigningKey()) // Sign with HS256 key
                .compact();              // Build and serialize to compact string
    }

    /**
     * Extract the email (subject) from a JWT token.
     *
     * @param token JWT token string
     * @return Email stored in 'sub' claim
     */
    public String extractEmail(String token) {
        return parseClaims(token).getSubject();  // Get 'sub' claim value
    }

    /**
     * Validate a JWT token: checks signature integrity and expiry.
     *
     * @param token JWT token string to validate
     * @return true if token is valid and not expired
     */
    public boolean isTokenValid(String token) {
        try {
            parseClaims(token);   // Throws exception if invalid or expired
            return true;          // No exception = token is valid
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired: {}", e.getMessage());     // Token past expiry date
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Malformed JWT token: {}", e.getMessage());   // Invalid structure
            return false;
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage()); // Signature mismatch etc.
            return false;
        }
    }

    /**
     * Parse and verify the JWT token; extract all claims.
     * Throws JwtException if signature invalid or token expired.
     *
     * @param token JWT token string
     * @return Claims object with all payload fields
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())   // Set the verification key
                .build()
                .parseSignedClaims(token)      // Verify signature + expiry
                .getPayload();                 // Extract claims payload
    }
}

