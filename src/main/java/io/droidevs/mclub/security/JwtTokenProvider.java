package io.droidevs.mclub.security;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
@Component
public class JwtTokenProvider {
    @Value("${app.jwt.secret:defaultSecretKeyWhichNeedsToBeLongEnoughForHS256AlgorithmExtremallyLong1234}")
    private String jwtSecret;
    @Value("${app.jwt.expiration:86400000}")
    private int jwtExpirationMs;

    private SecretKey getSigningKey() { return Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8)); }

    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpirationMs);
        return Jwts.builder().subject(username).issuedAt(now).expiration(expiryDate)
                .signWith(getSigningKey()).compact();
    }
    public String getUsernameFromJWT(String token) {
        return Jwts.parser().verifyWith(getSigningKey()).build()
                .parseSignedClaims(token).getPayload().getSubject();
    }
    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }
}
