package com.angel.ecomerce.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Logout efectivo: tras revocar el token, la API lo rechaza en cada petición.
 *
 * <p>{@link #loggedOutTokenIsRejected()} cubre la regresión: la lista negra se
 * escribía en logout, pero el filtro no estaba registrado en la cadena de
 * seguridad, así que el token seguía funcionando hasta caducar.</p>
 */
class LogoutRevocationIntegrationTest extends IntegrationTestSupport {

    private record Session(String accessToken, String refreshToken) {
    }

    @Test
    void loggedOutTokenIsRejected() throws Exception {
        Session session = customerSession("logout");

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken())))
                .andExpect(status().isOk());

        logout(session.accessToken()).andExpect(status().isNoContent());

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(session.accessToken())))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.detail").value("Token revocado. Inicie sesión de nuevo."));
    }

    @Test
    void loggingOutDoesNotAffectOtherTokens() throws Exception {
        Session ana = customerSession("ana-logout");
        Session luis = customerSession("luis-logout");

        logout(ana.accessToken());

        mockMvc.perform(get("/users/me").header(HttpHeaders.AUTHORIZATION, bearer(luis.accessToken())))
                .andExpect(status().isOk());
    }

    @Test
    void loggedOutTokenCannotRefresh() throws Exception {
        Session session = customerSession("logout-refresh");
        logout(session.refreshToken());

        refresh(session.refreshToken()).andExpect(status().isUnauthorized());
    }

    @Test
    void logoutTwiceIsAccepted() throws Exception {
        Session session = customerSession("logout-twice");

        logout(session.accessToken()).andExpect(status().isNoContent());
        logout(session.accessToken()).andExpect(status().isNoContent());
    }

    private Session customerSession(String prefix) throws Exception {
        String email = prefix + "-" + suffix() + "@example.com";
        createUserWithRole(email, "CUSTOMER");

        String body = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secreto123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        return new Session(
                JsonPath.read(body, "$.token"),
                JsonPath.read(body, "$.refreshToken"));
    }

    private org.springframework.test.web.servlet.ResultActions logout(String token) throws Exception {
        return mockMvc.perform(post("/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token":"%s"}
                        """.formatted(token)));
    }

    private org.springframework.test.web.servlet.ResultActions refresh(String refreshToken) throws Exception {
        return mockMvc.perform(post("/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"refreshToken":"%s"}
                        """.formatted(refreshToken)));
    }

    private String bearer(String token) {
        return "Bearer " + token;
    }
}