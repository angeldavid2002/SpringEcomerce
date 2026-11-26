package com.angel.ecomerce.service.auth;

import com.angel.ecomerce.dto.auth.LoginRequest;
import com.angel.ecomerce.dto.auth.LogoutRequest;
import com.angel.ecomerce.dto.auth.RefreshRequest;
import com.angel.ecomerce.dto.auth.RefreshResponse;
import com.angel.ecomerce.dto.auth.RegisterRequest;
import com.angel.ecomerce.dto.auth.TokenResponse;
import com.angel.ecomerce.entity.TokenBlacklist;
import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.entity.enums.Role;
import com.angel.ecomerce.repository.TokenBlacklistRepository;
import com.angel.ecomerce.security.JwtProperties;
import com.angel.ecomerce.service.users.IUser;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

/**
 * Valida credenciales y emite el token.
 *
 * <p>Email inexistente, contraseña incorrecta y usuario inactivo producen el
 * mismo error para no revelar qué cuentas existen.</p>
 */
@Service
@Transactional(readOnly = true)
public class AuthService implements IAuth {

    /** Vigencia del refresh token. El access token usa {@code app.jwt.expiration}. */
    private static final Duration REFRESH_TOKEN_TTL = Duration.ofDays(7);

    private final IUser usersService;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties properties;
    private final TokenBlacklistRepository blacklistRepository;

    public AuthService(IUser usersService, PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder, JwtDecoder jwtDecoder,
                       JwtProperties properties,
                       TokenBlacklistRepository blacklistRepository) {
        this.usersService = usersService;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.jwtDecoder = jwtDecoder;
        this.properties = properties;
        this.blacklistRepository = blacklistRepository;
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        User user = usersService.findByEmail(request.email())
                .filter(User::isActive)
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw invalidCredentials();
        }

        return issueTokenPair(user);
    }

    @Override
    @Transactional
    public TokenResponse register(RegisterRequest request) {
        User user = new User();
        user.setName(request.name());
        user.setLastName(request.lastName());
        user.setEmail(request.email());
        user.setPassword(request.password());
        user.setRole(Role.CUSTOMER);

        User saved = usersService.save(user);
        return issueTokenPair(saved);
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        String jti = extractJti(request.token());
        Instant expiresAt = extractExpiration(request.token());

        if (jti != null && expiresAt != null && blacklistRepository.findByJti(jti).isEmpty()) {
            blacklistRepository.save(blacklistEntry(jti, expiresAt));
        }
    }

    @Override
    @Transactional
    public RefreshResponse refresh(RefreshRequest request) {
        Jwt refreshToken;
        try {
            refreshToken = jwtDecoder.decode(request.refreshToken());
        } catch (JwtException e) {
            throw new BadCredentialsException("Refresh token inválido");
        }

        String jti = refreshToken.getClaimAsString("jti");
        Instant expiresAt = refreshToken.getExpiresAt();
        String type = refreshToken.getClaimAsString("type");

        if (!"refresh".equals(type)) {
            throw new BadCredentialsException("El token no es un refresh token");
        }

        if (jti == null || jti.isBlank()) {
            throw new BadCredentialsException("Refresh token sin identificador");
        }

        if (blacklistRepository.findByJti(jti).isPresent()) {
            throw new BadCredentialsException("Refresh token revocado");
        }

        Instant now = Instant.now();
        if (expiresAt == null || now.isAfter(expiresAt)) {
            throw new BadCredentialsException("Refresh token expirado");
        }

        String subject = refreshToken.getSubject();
        User user = usersService.findByEmail(subject)
                .filter(User::isActive)
                .orElseThrow(() -> new BadCredentialsException("Usuario del token ya no existe"));

        // Rotación: el refresh token usado se revoca antes de emitir el nuevo par.
        blacklistRepository.save(blacklistEntry(jti, expiresAt));

        TokenPair tokens = issueTokens(user, now);
        return new RefreshResponse(tokens.accessToken(), tokens.refreshToken(), "Bearer",
                properties.getExpiration().getSeconds());
    }

    private TokenResponse issueTokenPair(User user) {
        TokenPair tokens = issueTokens(user, Instant.now());
        return TokenResponse.bearerWithRefresh(tokens.accessToken(), tokens.refreshToken(),
                properties.getExpiration().getSeconds());
    }

    /**
     * Emite un par de tokens nuevos. Cada token lleva un {@code jti} único para
     * poder revocarlo (logout) y para que la rotación funcione en cadena.
     */
    private TokenPair issueTokens(User user, Instant issuedAt) {
        JwsHeader header = JwsHeader.with(properties.getAlgorithm()).build();

        String accessJti = UUID.randomUUID().toString();
        JwtClaimsSet accessClaims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(user.getEmail())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.getExpiration()))
                .claim("jti", accessJti)
                .claim("roles", List.of(user.getRole().name()))
                .build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, accessClaims)).getTokenValue();

        String refreshJti = UUID.randomUUID().toString();
        JwtClaimsSet refreshClaims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(user.getEmail())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(REFRESH_TOKEN_TTL))
                .claim("jti", refreshJti)
                .claim("roles", List.of(user.getRole().name()))
                .claim("type", "refresh")
                .build();
        String refreshToken = jwtEncoder.encode(JwtEncoderParameters.from(header, refreshClaims)).getTokenValue();

        return new TokenPair(accessToken, refreshToken);
    }

    private TokenBlacklist blacklistEntry(String jti, Instant expiresAt) {
        TokenBlacklist entry = new TokenBlacklist();
        entry.setJti(jti);
        entry.setExpiresAt(LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()));
        return entry;
    }

    private record TokenPair(String accessToken, String refreshToken) {
    }

    private BadCredentialsException invalidCredentials() {
        return new BadCredentialsException("Credenciales inválidas");
    }

    private String extractJti(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getClaimAsString("jti");
        } catch (JwtException e) {
            return null;
        }
    }

    private Instant extractExpiration(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getExpiresAt();
        } catch (JwtException e) {
            return null;
        }
    }
}
