package com.angel.ecomerce.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Recuperación de contraseña sin sesión: se pide un token por email y se
 * restablece la contraseña.
 *
 * <p>{@link #forgotAndResetPasswordWorkWithoutAuthentication()} cubre la
 * regresión: ambas rutas estaban fuera de la matriz de seguridad y respondían
 * {@code 401}, así que nadie podía recuperar su cuenta.</p>
 */
class PasswordRecoveryIntegrationTest extends IntegrationTestSupport {

    @Test
    void forgotAndResetPasswordWorkWithoutAuthentication() throws Exception {
        String email = "reset-" + suffix() + "@example.com";
        createUserWithRole(email, "CUSTOMER");

        String resetToken = requestResetToken(email);

        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s","password":"Nueva1234!"}
                                """.formatted(resetToken)))
                .andExpect(status().isNoContent());

        login(email, "Nueva1234!").andExpect(status().isOk());
        login(email, "secreto123").andExpect(status().isUnauthorized());
    }

    @Test
    void reusedResetTokenIsRejected() throws Exception {
        String email = "reset-once-" + suffix() + "@example.com";
        createUserWithRole(email, "CUSTOMER");

        String resetToken = requestResetToken(email);
        resetPassword(resetToken).andExpect(status().isNoContent());
        resetPassword(resetToken).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El token ya fue utilizado"));
    }

    @Test
    void resetWithInvalidTokenReturns400() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"no-existe","password":"Nueva1234!"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Token de recuperación inválido"));
    }

    @Test
    void forgotWithUnknownEmailReturns400() throws Exception {
        mockMvc.perform(post("/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nadie-%s@example.com"}
                                """.formatted(suffix())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("No existe un usuario activo con ese email"));
    }

    @Test
    void resetWithBlankPasswordReturns400WithFieldError() throws Exception {
        mockMvc.perform(post("/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"algo","password":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").exists());
    }

    private String requestResetToken(String email) throws Exception {
        return JsonPath.read(forgotPassword(email)
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.token");
    }

    private org.springframework.test.web.servlet.ResultActions forgotPassword(String email) throws Exception {
        return mockMvc.perform(post("/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s"}
                        """.formatted(email)));
    }

    private org.springframework.test.web.servlet.ResultActions resetPassword(String token) throws Exception {
        return mockMvc.perform(post("/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token":"%s","password":"Otra1234!"}
                        """.formatted(token)));
    }

    private org.springframework.test.web.servlet.ResultActions login(String email, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email":"%s","password":"%s"}
                        """.formatted(email, password)));
    }
}