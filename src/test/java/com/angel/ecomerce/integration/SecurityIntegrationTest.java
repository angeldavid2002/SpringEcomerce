package com.angel.ecomerce.integration;

import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.entity.enums.Role;
import com.angel.ecomerce.repository.UsersRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MvcResult;

import java.util.ArrayList;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Contrato de seguridad: qué es público, qué exige token y qué exige ADMIN.
 */
class SecurityIntegrationTest extends IntegrationTestSupport {

    @Autowired
    private UsersRepository usersRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Test
    void publicCatalogReadDoesNotRequireToken() throws Exception {
        mockMvc.perform(get("/products"))
                .andExpect(status().isOk());
    }

    @Test
    void anonymousRequestReturns401ProblemDetail() throws Exception {
        mockMvc.perform(get("/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("No autorizado"))
                .andExpect(jsonPath("$.detail").value("Se requiere un token válido"));
    }

    @Test
    void customerTokenOnAdminEndpointReturns403ProblemDetail() throws Exception {
        mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor("customer@test.local", "CUSTOMER")))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.title").value("Prohibido"))
                .andExpect(jsonPath("$.detail").value("No tiene permisos para acceder a este recurso"));
    }

    @Test
    void customerTokenCanManageOwnOrdersScope() throws Exception {
        mockMvc.perform(get("/orders/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor("customer@test.local", "CUSTOMER")))
                .andExpect(status().isNotFound());
    }

    @Test
    void customerTokenReadsOwnProfile() throws Exception {
        String email = "me-" + suffix() + "@example.com";
        createUserWithRole(email, "CUSTOMER");

        mockMvc.perform(get("/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + tokenFor(email, "CUSTOMER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.role").value("CUSTOMER"));
    }

    @Test
    void anonymousProfileRequestReturns401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void adminTokenReachesAdminEndpoints() throws Exception {
        mockMvc.perform(get("/users")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + adminToken()))
                .andExpect(status().isOk());
    }

    @Test
    void loginIssuesTokenForActiveUser() throws Exception {
        String email = "login-" + suffix() + "@example.com";

        User user = new User();
        user.setName("Login");
        user.setLastName("Prueba");
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode("secreto123"));
        user.setRole(Role.CUSTOMER);
        user.setActive(true);
        user.setAddresses(new ArrayList<>());
        usersRepository.save(user);

        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secreto123"}
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andReturn();

        String token = JsonPath.read(result.getResponse().getContentAsString(), "$.token");
        mockMvc.perform(get("/orders/999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void loginWithWrongPasswordReturns401() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"nadie@example.com","password":"secreto123"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("Credenciales inválidas"));
    }
}
