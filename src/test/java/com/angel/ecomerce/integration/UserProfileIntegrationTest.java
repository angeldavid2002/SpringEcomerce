package com.angel.ecomerce.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Perfil propio ({@code PATCH /users/me}): actualización parcial sin contraseña
 * ni rol, y un email nuevo pierde la verificación.
 *
 * <p>{@link #patchWithOnlyNameUpdatesJustTheName()} cubre la regresión: el
 * cuerpo exigía {@code email} y un PATCH parcial respondía {@code 400}.</p>
 */
class UserProfileIntegrationTest extends IntegrationTestSupport {

    @Test
    void patchWithOnlyNameUpdatesJustTheName() throws Exception {
        String email = "perfil-nombre-" + suffix() + "@example.com";
        String token = createCustomerWithToken(email);

        mockMvc.perform(patch("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"SoloNombre"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("SoloNombre"))
                .andExpect(jsonPath("$.email").value(email));
    }

    @Test
    void changingEmailResetsVerification() throws Exception {
        String email = "perfil-email-" + suffix() + "@example.com";
        String token = createCustomerWithToken(email);
        verifyEmail(token);

        String newEmail = "nuevo-" + email;
        mockMvc.perform(patch("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(newEmail)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(newEmail))
                .andExpect(jsonPath("$.emailVerified").value(false));
    }

    @Test
    void patchWithEmailInUseReturns400() throws Exception {
        String email = "perfil-dup-" + suffix() + "@example.com";
        String otherEmail = "otro-" + suffix() + "@example.com";
        String token = createCustomerWithToken(email);
        createUserWithRole(otherEmail, "CUSTOMER");

        mockMvc.perform(patch("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(otherEmail)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Ya existe un usuario con ese email"));
    }

    @Test
    void patchCannotChangeRoleOrPassword() throws Exception {
        String email = "perfil-rol-" + suffix() + "@example.com";
        String token = createCustomerWithToken(email);

        mockMvc.perform(patch("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"role":"ADMIN","password":"otra","active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.active").value(true));

        // La contraseña sigue siendo la del alta.
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secreto123"}
                                """.formatted(email)))
                .andExpect(status().isOk());
    }

    @Test
    void patchWithoutTokenReturns401() throws Exception {
        mockMvc.perform(patch("/users/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Nadie"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private String createCustomerWithToken(String email) throws Exception {
        createUserWithRole(email, "CUSTOMER");
        return tokenFor(email, "CUSTOMER");
    }

    private void verifyEmail(String token) throws Exception {
        String verificationToken = JsonPath.read(mockMvc.perform(post("/auth/verify-email")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.token");

        mockMvc.perform(post("/auth/verify-email/confirm")
                        .header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"%s"}
                                """.formatted(verificationToken)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}