package com.angel.ecomerce.controller;

import com.angel.ecomerce.dto.auth.ForgotPasswordRequest;
import com.angel.ecomerce.dto.auth.ResetPasswordRequest;
import com.angel.ecomerce.dto.auth.VerifyEmailRequest;
import com.angel.ecomerce.security.OwnershipGuard;
import com.angel.ecomerce.service.email.EmailVerificationService;
import com.angel.ecomerce.service.passwordreset.PasswordResetService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthProfileController {
    private final OwnershipGuard ownership;
    private final EmailVerificationService emailVerificationService;
    private final PasswordResetService passwordResetService;

    public AuthProfileController(OwnershipGuard ownership,
                                 EmailVerificationService emailVerificationService,
                                 PasswordResetService passwordResetService) {
        this.ownership = ownership;
        this.emailVerificationService = emailVerificationService;
        this.passwordResetService = passwordResetService;
    }

    /**
     * Genera un token de verificación de email para el usuario autenticado.
     * Devuelve el token en la respuesta (en producción se enviaría por email).
     */
    @PostMapping("/verify-email")
    public ResponseEntity<VerifyEmailResponse> requestVerifyEmail() {
        Long userId = ownership.currentUserId();
        String token = emailVerificationService.createVerificationToken(userId);
        return ResponseEntity.ok(new VerifyEmailResponse(token));
    }

    /**
     * Verifica el email usando el token recibido.
     * Cuerpo: { "token": "..." }
     */
    @PostMapping("/verify-email/confirm")
    public ResponseEntity<Void> confirmVerifyEmail(@Valid @RequestBody VerifyEmailRequest request) {
        emailVerificationService.verifyEmail(request.token());
        return ResponseEntity.noContent().build();
    }

    /**
     * Solicita restablecimiento de contraseña. Recibe el email y devuelve el
     * token de restablecimiento (en producción se enviaría por email).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ForgotPasswordResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        String token = passwordResetService.createResetToken(request.email());
        return ResponseEntity.ok(new ForgotPasswordResponse(token));
    }

    /**
     * Restablece la contraseña usando el token recibido.
     * Cuerpo: { "token": "...", "password": "..." }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.password());
        return ResponseEntity.noContent().build();
    }

    // --- DTOs de respuesta locales ---

    public record VerifyEmailResponse(String token) {
    }

    public record ForgotPasswordResponse(String token) {
    }
}
