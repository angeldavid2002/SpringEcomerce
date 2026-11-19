package com.angel.ecomerce.controller;

import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.service.users.IUser;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static com.angel.ecomerce.support.TestFixtures.user;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UsersController.class)
class UsersControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IUser usersService;

    // --- Creación ---

    @Test
    void createReturns201WithDefaultRoleAndActiveState() throws Exception {
        given(usersService.save(any(User.class)))
                .willReturn(user(1L, "ana@example.com"));

        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ana","lastName":"Pérez","email":"ana@example.com","password":"secreto123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ana@example.com"))
                .andExpect(jsonPath("$.role").value("CUSTOMER"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void createWithInvalidEmailReturns400() throws Exception {
        mockMvc.perform(post("/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Ana","email":"no-es-un-email","password":"secreto123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").value("El email no tiene un formato válido"));
    }

    // --- Consultas ---

    @Test
    void getByEmailReturnsUser() throws Exception {
        given(usersService.findByEmail("ana@example.com"))
                .willReturn(Optional.of(user(1L, "ana@example.com")));

        mockMvc.perform(get("/users/by-email").param("email", "ana@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@example.com"));
    }

    @Test
    void getByIdReturns404WhenMissing() throws Exception {
        given(usersService.findById(99L)).willReturn(Optional.empty());

        mockMvc.perform(get("/users/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("No existe un usuario con ese id"));
    }

    // --- Desactivación y borrado ---

    @Test
    void deactivateReturnsUpdatedUser() throws Exception {
        given(usersService.findById(1L))
                .willReturn(Optional.of(user(1L, "ana@example.com", false)));

        mockMvc.perform(patch("/users/1/deactivate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));

        verify(usersService).deactivateById(1L);
    }

    @Test
    void deleteReturns409WhenUserHasOrders() throws Exception {
        doThrow(new IllegalStateException(
                "No se puede eliminar el usuario porque tiene pedidos asociados; desactívelo en su lugar"))
                .when(usersService).deleteById(1L);

        mockMvc.perform(delete("/users/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }
}
