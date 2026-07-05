package tiameds.pharmabackend.service.impl;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Service
public class JwtService {

    @Value("${jwt.secret-key}")
    private String secret;

    @Value("${jwt.access-token-expiry}")
    private long accessExpiry;

    @Value("${jwt.refresh-token-expiry}")
    private long refreshExpiry;

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generateAccessToken(
            tiameds.pharmabackend.entity.UserDetails user) {

        return Jwts.builder()
                .subject(user.getUserEmail())
                .claim("userId", user.getUserId())
                .claim("role", user.getRole().getRoleName())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessExpiry))
                .signWith(getKey())
                .compact();
    }

    public String generateRefreshToken(
            tiameds.pharmabackend.entity.UserDetails user) {

        return Jwts.builder()
                .subject(user.getUserEmail())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + refreshExpiry))
                .signWith(getKey())
                .compact();
    }

    private Claims getClaims(String token) {

        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public String extractUsername(String token) {

        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {

        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(
            String token,
            Function<Claims, T> resolver) {

        return resolver.apply(getClaims(token));
    }

    public boolean isTokenExpired(String token) {

        return extractExpiration(token).before(new Date());
    }

//    public boolean validateToken(
//            String token,
//            UserDetails userDetails) {
//
//        String username = extractUsername(token);
//
//        return username.equals(userDetails.getUsername())
//                && !isTokenExpired(token);
//    }


    public boolean validateToken(
            String token,
            UserDetails userDetails) {

        String username = extractUsername(token);

        System.out.println("========== VALIDATE TOKEN ==========");
        System.out.println("JWT Username      : " + username);
        System.out.println("Spring Username   : " + userDetails.getUsername());
        System.out.println("Expired           : " + isTokenExpired(token));
        System.out.println("Equals            : " + username.equals(userDetails.getUsername()));
        System.out.println("====================================");

        return username.equals(userDetails.getUsername())
                && !isTokenExpired(token);
    }
}