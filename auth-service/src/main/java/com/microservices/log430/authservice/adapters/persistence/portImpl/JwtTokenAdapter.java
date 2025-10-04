package com.microservices.log430.authservice.adapters.persistence.portImpl;

import com.microservices.log430.authservice.domain.port.out.JwtTokenPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtTokenAdapter implements JwtTokenPort {

    private final SecretKey secretKey;
    private final long jwtExpiration;

    public JwtTokenAdapter(@Value("${jwt.secret:myDefaultSecretKeyThatIsAtLeast32CharsLong}") String secret,
                           @Value("${jwt.expiration:86400000}") long jwtExpiration) { // 24h par défaut
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.jwtExpiration = jwtExpiration;
    }

    @Override
    public String generateToken(Long userId, String email) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setSubject(userId.toString())
                .claim("email", email)
                .claim("role", "CLIENT")  // ← Rôle CLIENT hardcodé pour respecter l'UC-02
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(secretKey, SignatureAlgorithm.HS256)
                .compact();
    }

    @Override
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                    .setSigningKey(secretKey)
                    .build()
                    .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return Long.parseLong(claims.getSubject());
    }

    @Override
    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("email", String.class);
    }

    @Override
    public String getRoleFromToken(String token) {
        Claims claims = Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();

        return claims.get("role", String.class);
    }
}
