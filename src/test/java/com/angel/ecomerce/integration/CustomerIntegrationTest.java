package com.angel.ecomerce.integration;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CustomerIntegrationTest extends IntegrationTestSupport {

    @Test
    void userEmailIsNormalizedAndRoleDefaultsToCustomer() throws Exception {
        String suffix = suffix();
        String rawEmail = "User-" + suffix + "@Example.COM";
        String normalizedEmail = rawEmail.toLowerCase(Locale.ROOT);

        MvcResult result = createUserWithRole(rawEmail, null).andReturn();

        assertEquals(normalizedEmail, JsonPath.read(result.getResponse().getContentAsString(), "$.email"));
        assertEquals("CUSTOMER", JsonPath.read(result.getResponse().getContentAsString(), "$.role"));
        assertTrue((Boolean) JsonPath.read(result.getResponse().getContentAsString(), "$.active"));

        mockMvc.perform(get("/users/by-email").param("email", rawEmail.toUpperCase(Locale.ROOT)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(normalizedEmail));
    }

    @Test
    void duplicateEmailIsRejected() throws Exception {
        String suffix = suffix();
        String email = "dup-" + suffix + "@example.com";
        createUserWithRole(email, "CUSTOMER");

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Otra","email":"%s","password":"secreto123"}
                                """.formatted(email.toUpperCase(Locale.ROOT))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Ya existe un usuario con ese email"));
    }

    @Test
    void addressBelongsToUserAndIsProtectedWhenUsedByOrder() throws Exception {
        String suffix = suffix();
        long ownerId = createUser("buyer-" + suffix);
        long otherId = createUser("other-" + suffix);
        long addressId = createAddress(ownerId);

        mockMvc.perform(get("/addresses/{id}/user/{userId}", addressId, otherId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").isNotEmpty());

        createOrder(ownerId, addressId);

        mockMvc.perform(delete("/addresses/{id}", addressId))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }
}
