package tiameds.pharmabackend.service.impl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tiameds.pharmabackend.entity.UserDetails;

import java.security.Key;
import java.util.Date;

@Service
public class JwtService {

    @Value("${jwt.secret-key}")
    private String secret;

    @Value("${jwt.access-token-expiry}")
    private long accessExpiry;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshExpiry;

    private Key getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(UserDetails user) {

        return Jwts.builder()
                .subject(user.getUserName())
                .claim("userId", user.getUserId())
                .claim("role", user.getRole().getRoleName())
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()+accessExpiry))
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(UserDetails user) {

        return Jwts.builder()
                .subject(user.getUserName())
                .issuedAt(new Date())
                .expiration(
                        new Date(
                                System.currentTimeMillis()+refreshExpiry))
                .signWith(getKey())
                .compact();
    }

    public String extractUsername(String token){

        return Jwts.parser()
                .verifyWith((javax.crypto.SecretKey) getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .getSubject();
    }

    public boolean isTokenExpired(String token){

        Date expiration =
                Jwts.parser()
                        .verifyWith((javax.crypto.SecretKey) getKey())
                        .build()
                        .parseSignedClaims(token)
                        .getPayload()
                        .getExpiration();

        return expiration.before(new Date());
    }
}
