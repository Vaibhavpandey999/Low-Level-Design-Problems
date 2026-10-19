package com.carrental.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

/**
 * JwtService – creates and validates JWT tokens for stateless authentication.
 *
 * JWT structure: header.payload.signature
 * - header: algorithm type (HS256)
 * - payload: claims (subject=email, issued_at, expiration)
 * - signature: HMAC-SHA256 of header+payload using the secret key
 *
 * The secret key is loaded from application.yml (never hardcoded).
 * The token is sent in the Authorization header: "Bearer {token}"
 */
@Service  // Spring-managed singleton
@Slf4j
public class JwtService {

    // Secret key loaded from application.yml – must be at least 256 bits for HS256
    @Value("${app.jwt.secret}")
    private String jwtSecret;

    // Token validity period loaded from application.yml (e.g., 86400000 = 24 hours in ms)
    @Value("${app.jwt.expiration-ms}")
    private long jwtExpirationMs;

    /**
     * Generate a JWT token for the authenticated user.
     *
     * @param username the user's email (used as JWT subject)
     * @return signed JWT string to return in the login response
     */
    public String generateToken(String username) {
        return Jwts.builder()
                .subject(username)                         // "sub" claim = user email
                .issuedAt(new Date())                      // "iat" claim = current time
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs)) // "exp" claim
                .signWith(getSigningKey())                 // HMAC-SHA256 signature
                .compact();                                // serialise to string
    }

    /**
     * Extract the username (email) from a JWT token.
     *
     * @param token JWT string from Authorization header
     * @return email of the token's owner
     */
    public String extractUsername(String token) {
        return parseClaims(token).getSubject();  // get "sub" claim value
    }

    /**
     * Validate that a token is well-formed, properly signed, and not expired.
     *
     * @param token    JWT string to validate
     * @param username expected subject to verify token ownership
     * @return true if token is valid and belongs to the given user
     */
    public boolean isTokenValid(String token, String username) {
        try {
            String extractedUsername = extractUsername(token);  // throws if signature invalid
            return extractedUsername.equals(username) && !isTokenExpired(token);  // check both
        } catch (JwtException ex) {
            log.warn("[JwtService] Invalid token: {}", ex.getMessage());
            return false;  // any JWT exception means invalid
        }
    }

    /**
     * Check if a token's expiration claim is in the past.
     *
     * @param token JWT string to check
     * @return true if the token has expired
     */
    private boolean isTokenExpired(String token) {
        Date expiration = parseClaims(token).getExpiration();  // extract "exp" claim
        return expiration.before(new Date());                   // compare to current time
    }

    /**
     * Parse and return the Claims (payload) from a JWT token.
     * Verifies the signature and throws JwtException on tampered tokens.
     *
     * @param token JWT string
     * @return Claims object with all payload values
     */
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey())    // set key for signature verification
                .build()
                .parseSignedClaims(token)       // parse + verify; throws on tamper/expiry
                .getPayload();                  // return claims payload
    }

    /**
     * Convert the Base64-encoded secret string to a cryptographic SecretKey.
     * Keys.hmacShaKeyFor() validates minimum key length for HS256.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);  // decode Base64 secret
        return Keys.hmacShaKeyFor(keyBytes);                   // create HMAC-SHA key
    }
}

