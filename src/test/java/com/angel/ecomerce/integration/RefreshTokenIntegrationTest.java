package com.angel.ecomerce.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Renovación de tokens: rotación en cadena, rechazo de tokens ya usados y del
 * access token usado como refresh.
 *
 * <p>El caso {@link #refreshRotatesTokensAndNewRefreshTokenKeepsWorking()} cubre
 * la regresión: el token emitido por {@code /auth/refresh} no llevaba {@code jti}
 * y el segundo refresco fallaba.</p>
 */
class RefreshTokenIntegrationTest extends IntegrationTestSupport {

    private record Session(String accessToken, String refreshToken) {
    }

    @Test
    void refreshRotatesTokensAndNewRefreshTokenKeepsWorking() throws Exception {
        Session first = login(customer("refresh-chain"));

        MvcResult rotated = refresh(first.refreshToken())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String accessToken = JsonPath.read(rotated.getResponse().getContentAsString(), "$.token");
        String refreshToken = JsonPath.read(rotated.getResponse().getContentAsString(), "$.refreshToken");

        // El access token renovado sirve en un endpoint protegido.
        mockMvc.perform(get("/cart").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk());

        // El refresh token rotado se puede volver a usar (regresión).
        refresh(refreshToken).andExpect(status().isOk());
    }

    @Test
    void usedRefreshTokenIsRejectedAfterRotation() throws Exception {
        Session session = login(customer("refresh-reuse"));

        refresh(session.refreshToken()).andExpect(status().isOk());
        refresh(session.refreshToken()).andExpect(status().isUnauthorized());
    }

    @Test
    void accessTokenIsNotAcceptedAsRefreshToken() throws Exception {
        Session session = login(customer("refresh-access"));

        refresh(session.accessToken()).andExpect(status().isUnauthorized());
    }

    @Test
    void malformedRefreshTokenReturns401() throws Exception {
        refresh("esto-no-es-un-jwt").andExpect(status().isUnauthorized());
    }

    @Test
    void missingRefreshTokenReturns400WithFieldError() throws Exception {
        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.refreshToken").exists());
    }

    private String customer(String prefix) throws Exception {
        String email = prefix + "-" + suffix() + "@example.com";
        createUserWithRole(email, "CUSTOMER");
        return email;
    }

    private Session login(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secreto123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andReturn();

        String body = result.getResponse().getContentAsString();
        return new Session(
                JsonPath.read(body, "$.token"),
                JsonPath.read(body, "$.refreshToken"));
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken":"%s"}
                        """.formatted(refreshToken)));
    }
}
