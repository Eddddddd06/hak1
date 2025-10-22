package com.example.hack1.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

public class JwtTokenService {

    private final SecretKey secretKey;
    private final long ttlSeconds;

    public JwtTokenService(String base64Secret, long ttlSeconds) {
        byte[] keyBytes = Decoders.BASE64.decode(base64Secret);
        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.ttlSeconds = ttlSeconds;
    }

    public String createToken(String username, String role, String branch) {
        Instant now = Instant.now();
        Instant exp = now.plusSeconds(ttlSeconds);
        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(exp))
                .addClaims(Map.of(
                        "role", role,
                        "branch", branch
                ))
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    public JwtPayload validateAndParse(String token) {
        var jwt = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token);
        var claims = jwt.getBody();
        String username = claims.getSubject();
        String role = claims.get("role", String.class);
        String branch = claims.get("branch", String.class);
        return new JwtPayload(username, role, branch);
    }

    public record JwtPayload(String username, String role, String branch) {}
}