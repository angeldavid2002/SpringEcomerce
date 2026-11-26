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

        if (jti != null && expiresAt != null && !blacklistRepository.findByJti(jti).isPresent()) {
            TokenBlacklist entry = new TokenBlacklist();
            entry.setJti(jti);
            entry.setExpiresAt(LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()));
            blacklistRepository.save(entry);
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

        if (blacklistRepository.findByJti(jti).isPresent()) {
            throw new BadCredentialsException("Refresh token revocado");
        }

        Instant now = Instant.now();
        if (now.isAfter(expiresAt)) {
            throw new BadCredentialsException("Refresh token expirado");
        }

        String subject = refreshToken.getSubject();
        User user = usersService.findByEmail(subject)
                .filter(User::isActive)
                .orElseThrow(() -> new BadCredentialsException("Usuario del token ya no existe"));

        // Revocar el refresh token usado
        TokenBlacklist entry = new TokenBlacklist();
        entry.setJti(jti);
        entry.setExpiresAt(LocalDateTime.ofInstant(expiresAt, ZoneId.systemDefault()));
        blacklistRepository.save(entry);

        // Emitir nuevo par de tokens
        Instant issuedAt = Instant.now();
        JwtClaimsSet accessClaims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(user.getEmail())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.getExpiration()))
                .claim("roles", List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(properties.getAlgorithm()).build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, accessClaims)).getTokenValue();

        Instant refreshExpiresAt = issuedAt.plus(Duration.ofDays(7));
        JwtClaimsSet refreshClaims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(user.getEmail())
                .issuedAt(issuedAt)
                .expiresAt(refreshExpiresAt)
                .claim("roles", List.of(user.getRole().name()))
                .claim("type", "refresh")
                .build();
        String newRefreshToken = jwtEncoder.encode(JwtEncoderParameters.from(header, refreshClaims)).getTokenValue();

        return RefreshResponse.bearer(accessToken, newRefreshToken, properties.getExpiration().getSeconds());
    }

    private TokenResponse issueTokenPair(User user) {
        Instant issuedAt = Instant.now();

        // Access token con jti único para poder revocarlo en logout
        String accessJti = UUID.randomUUID().toString();
        JwtClaimsSet accessClaims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(user.getEmail())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.getExpiration()))
                .claim("jti", accessJti)
                .claim("roles", List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(properties.getAlgorithm()).build();

        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, accessClaims)).getTokenValue();

        // Refresh token (más duradero, marcado como tipo refresh, con jti único)
        String refreshJti = UUID.randomUUID().toString();
        Instant refreshExpiresAt = issuedAt.plus(Duration.ofDays(7));
        JwtClaimsSet refreshClaims = JwtClaimsSet.builder()
                .issuer(properties.getIssuer())
                .subject(user.getEmail())
                .issuedAt(issuedAt)
                .expiresAt(refreshExpiresAt)
                .claim("jti", refreshJti)
                .claim("roles", List.of(user.getRole().name()))
                .claim("type", "refresh")
                .build();
        String refreshToken = jwtEncoder.encode(JwtEncoderParameters.from(header, refreshClaims)).getTokenValue();

        return TokenResponse.bearerWithRefresh(accessToken, refreshToken, properties.getExpiration().getSeconds());
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
