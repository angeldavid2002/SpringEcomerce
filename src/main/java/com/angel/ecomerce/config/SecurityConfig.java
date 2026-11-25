package com.angel.ecomerce.config;

import com.angel.ecomerce.security.JwtProperties;
import com.angel.ecomerce.security.RestAccessDeniedHandler;
import com.angel.ecomerce.security.RestAuthenticationEntryPoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.List;

/**
 * Seguridad de la API: Resource Server con JWT (HS256), sesiones sin estado y
 * autorización por rol declarada por ruta y verbo.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder(
            @Value("${app.security.bcrypt-strength:10}") int strength) {
        if (strength < 4 || strength > 31) {
            throw new IllegalArgumentException(
                    "app.security.bcrypt-strength (BCRYPT_STRENGTH) debe estar entre 4 y 31");
        }
        return new BCryptPasswordEncoder(strength);
    }

    @Bean
    public JwtEncoder jwtEncoder(JwtProperties properties) {
        return NimbusJwtEncoder.withSecretKey(secretKey(properties))
                .algorithm(properties.algorithm())
                .build();
    }

    @Bean
    public JwtDecoder jwtDecoder(JwtProperties properties) {
        return NimbusJwtDecoder.withSecretKey(secretKey(properties))
                .macAlgorithm(properties.algorithm())
                .build();
    }

    @Bean
    public Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter roles = new JwtGrantedAuthoritiesConverter();
        roles.setAuthoritiesClaimName("roles");
        roles.setAuthorityPrefix("ROLE_");

        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(roles);
        converter.setPrincipalClaimName("sub");
        return converter;
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.cors.allowed-origins:http://localhost:3000}") String allowedOrigins,
            @Value("${app.cors.allowed-methods:GET,POST,PUT,PATCH,DELETE,OPTIONS}") String allowedMethods,
            @Value("${app.cors.allowed-headers:*}") String allowedHeaders,
            @Value("${app.cors.allow-credentials:true}") boolean allowCredentials,
            @Value("${app.cors.max-age:3600}") long maxAge) {

        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        config.setAllowedMethods(Arrays.asList(allowedMethods.split(",")));
        config.setAllowedHeaders(Arrays.asList(allowedHeaders.split(",")));
        config.setAllowCredentials(allowCredentials);
        config.setMaxAge(maxAge);
        config.setExposedHeaders(List.of("Authorization", "Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   RestAuthenticationEntryPoint entryPoint,
                                                   RestAccessDeniedHandler accessDeniedHandler,
                                                   Converter<Jwt, AbstractAuthenticationToken> jwtAuthenticationConverter,
                                                   CorsConfigurationSource corsConfigurationSource)
            throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        .requestMatchers("/swagger", "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/categories", "/categories/**", "/products", "/products/**")
                        .permitAll()
                        .requestMatchers("/categories", "/categories/**", "/products", "/products/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/users/me").authenticated()
                        .requestMatchers("/inventories/**", "/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/addresses").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.GET, "/orders", "/orders/status/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/orders/*/status").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/orders/*").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(entryPoint)
                        .accessDeniedHandler(accessDeniedHandler));
        return http.build();
    }

    private SecretKey secretKey(JwtProperties properties) {
        String jcaName = switch (properties.algorithm()) {
            case HS256 -> "HmacSHA256";
            case HS384 -> "HmacSHA384";
            case HS512 -> "HmacSHA512";
        };
        return new SecretKeySpec(properties.secretBytes(), jcaName);
    }
}