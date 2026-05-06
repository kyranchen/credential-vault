package com.credentialvault.auth;

import com.credentialvault.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret}")
    private String jwtSecretBase64;

    @Value("${jwt.expiration-ms}")
    private long expirationMs;

    private SecretKey signingKey() {
        return Keys.hmacShaKeyFor(Base64.getDecoder().decode(jwtSecretBase64));
    }

    public String generateToken(User user) {
        return Jwts.builder()
            .subject(user.getId())
            .claim("orgId", user.getOrgId())
            .claim("role", user.getRole().name())
            .claim("email", user.getEmail())
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + expirationMs))
            .signWith(signingKey())
            .compact();
    }

    public Claims extractAllClaims(String token) {
        return Jwts.parser()
            .verifyWith(signingKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean isTokenValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public AuthenticatedUser toAuthenticatedUser(String token) {
        Claims claims = extractAllClaims(token);
        String userId = claims.getSubject();
        String orgId = claims.get("orgId", String.class);
        User.Role role = User.Role.valueOf(claims.get("role", String.class));
        return new AuthenticatedUser(userId, orgId, role);
    }
}
