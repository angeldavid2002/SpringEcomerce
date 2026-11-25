package com.angel.ecomerce.service.auth;

import com.angel.ecomerce.dto.auth.LoginRequest;
import com.angel.ecomerce.dto.auth.RegisterRequest;
import com.angel.ecomerce.dto.auth.TokenResponse;
import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.entity.enums.Role;
import com.angel.ecomerce.security.JwtProperties;
import com.angel.ecomerce.service.users.IUser;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

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
    private final JwtProperties properties;

    public AuthService(IUser usersService, PasswordEncoder passwordEncoder,
                       JwtEncoder jwtEncoder, JwtProperties properties) {
        this.usersService = usersService;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    @Override
    public TokenResponse login(LoginRequest request) {
        User user = usersService.findByEmail(request.email())
                .filter(User::isActive)
                .orElseThrow(this::invalidCredentials);

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw invalidCredentials();
        }

        return issueToken(user);
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
        return issueToken(saved);
    }

    private TokenResponse issueToken(User user) {
        Instant issuedAt = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(user.getEmail())
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(properties.expiration()))
                .claim("roles", List.of(user.getRole().name()))
                .build();
        JwsHeader header = JwsHeader.with(properties.algorithm()).build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return TokenResponse.bearer(token, properties.expiration());
    }

    private BadCredentialsException invalidCredentials() {
        return new BadCredentialsException("Credenciales inválidas");
    }
}
