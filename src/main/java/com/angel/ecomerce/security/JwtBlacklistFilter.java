package com.angel.ecomerce.security;

import com.angel.ecomerce.repository.TokenBlacklistRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtro que rechaza (401) cualquier petición cuyo JWT esté en la lista negra
 * de tokens revocados (logout). Se ejecuta una sola vez por petición.
 *
 * <p>Nota: no se declara como {@code @Component} para evitar romper los slice
 * tests. Para activarlo en producción, regístralo en
 * {@link com.angel.ecomerce.config.SecurityConfig} con {@code addFilterAfter}.</p>
 */
public class JwtBlacklistFilter extends OncePerRequestFilter {

    private final TokenBlacklistRepository blacklistRepository;

    public JwtBlacklistFilter(TokenBlacklistRepository blacklistRepository) {
        this.blacklistRepository = blacklistRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = org.springframework.security.core.context.SecurityContextHolder
                .getContext().getAuthentication();

        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            String jti = jwtAuth.getToken().getClaimAsString("jti");
            if (jti != null && blacklistRepository.findByJti(jti).isPresent()) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                response.setContentType("application/problem+json");
                response.getWriter().write("""
                        {
                          "type": "about:blank",
                          "title": "No autorizado",
                          "status": 401,
                          "detail": "Token revocado. Inicie sesión de nuevo.",
                          "instance": "%s"
                        }
                        """.formatted(request.getRequestURI()));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }
}
