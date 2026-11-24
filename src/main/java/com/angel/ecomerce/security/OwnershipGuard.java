package com.angel.ecomerce.security;

import com.angel.ecomerce.repository.UsersRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * Reglas de propiedad por recurso.
 *
 * <p>El rol se sigue comprobando en {@link com.angel.ecomerce.config.SecurityConfig}
 * por ruta y verbo; esta clase resuelve lo que una regla de ruta no puede: que un
 * {@code CUSTOMER} solo acceda a sus propios pedidos y direcciones. Un
 * {@code ADMIN} pasa siempre, sin consultar la base de datos.</p>
 *
 * <p>Cuando el recurso existe pero es de otro usuario se responde 404, no 403,
 * para no revelar que el id está ocupado.</p>
 */
@Component
public class OwnershipGuard {

    private final UsersRepository usersRepository;

    public OwnershipGuard(UsersRepository usersRepository) {
        this.usersRepository = usersRepository;
    }

    public boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    /**
     * Id del usuario autenticado, resuelto desde el claim {@code sub} (email).
     */
    public Long currentUserId() {
        String email = currentEmail();
        return usersRepository.findByEmailIgnoreCase(email)
                .map(user -> user.getId())
                .orElseThrow(() -> new AccessDeniedException("El usuario del token ya no existe"));
    }

    /**
     * Exige que el usuario autenticado sea el indicado, salvo que sea ADMIN.
     * Se usa cuando el id llega como parámetro de la petición (no del recurso).
     */
    public void requireSelfOrAdmin(Long userId) {
        if (isAdmin()) {
            return;
        }
        if (userId == null || !Objects.equals(userId, currentUserId())) {
            throw new AccessDeniedException("No puede operar sobre recursos de otro usuario");
        }
    }

    /**
     * Exige que el usuario autenticado sea el dueño del recurso, salvo que sea ADMIN.
     * Lanza {@link NoSuchElementException} para que la respuesta sea 404 y no filtre
     * la existencia del recurso ajeno.
     */
    public void requireOwnerOrAdmin(Long ownerId) {
        if (isAdmin()) {
            return;
        }
        if (ownerId == null || !Objects.equals(ownerId, currentUserId())) {
            throw new NoSuchElementException("El recurso solicitado no existe");
        }
    }

    private String currentEmail() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
            return jwtAuthentication.getToken().getSubject();
        }
        throw new AccessDeniedException("Se requiere un token válido");
    }
}
