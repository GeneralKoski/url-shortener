package dev.koski.urlshortener.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import dev.koski.urlshortener.model.User;
import dev.koski.urlshortener.repository.UserRepository;
import dev.koski.urlshortener.web.TokenResponse;

@Service
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final Duration ttl;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder,
            @Value("${app.jwt.ttl:1h}") Duration ttl) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.ttl = ttl;
        this.dummyHash = passwordEncoder.encode("dummy-password");
    }

    public String register(String username, String password) {
        String name = normalize(username);
        try {
            users.insert(new User(null, name, passwordEncoder.encode(password), Instant.now()));
        } catch (DuplicateKeyException e) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username già in uso");
        }
        return name;
    }

    public TokenResponse login(String username, String password) {
        User user = users.findByUsername(normalize(username)).orElse(null);
        boolean valid = passwordEncoder.matches(password, user != null ? user.passwordHash() : dummyHash);
        if (user == null || !valid) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenziali non valide");
        }
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.username())
                .issuedAt(now)
                .expiresAt(now.plus(ttl))
                .build();
        String token = jwtEncoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
        return new TokenResponse(token, "Bearer", ttl.toSeconds());
    }

    private String normalize(String username) {
        return username.toLowerCase(Locale.ROOT);
    }
}
