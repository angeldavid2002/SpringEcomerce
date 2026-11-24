package com.angel.ecomerce.controller;

import com.angel.ecomerce.entity.Address;
import com.angel.ecomerce.entity.User;
import com.angel.ecomerce.security.OwnershipGuard;
import com.angel.ecomerce.service.address.IAddress;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static com.angel.ecomerce.support.TestFixtures.address;
import static com.angel.ecomerce.support.TestFixtures.user;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AddressesController.class)
@AutoConfigureMockMvc(addFilters = false)
class AddressesControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private IAddress addressService;

    @MockitoBean
    private OwnershipGuard ownershipGuard;

    // --- Creación ---

    @Test
    void createReturns201WithUser() throws Exception {
        User user = user(1L, "ana@example.com");
        given(addressService.save(eq(1L), any(Address.class)))
                .willReturn(address(10L, user));

        mockMvc.perform(post("/addresses")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"street":"Calle 1","city":"Ciudad","state":"Estado","zipCode":"12345","country":"País"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.street").value("Calle 1"));
    }

    @Test
    void createWithBlankStreetReturns400() throws Exception {
        mockMvc.perform(post("/addresses")
                        .param("userId", "1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"street":"","city":"Ciudad","country":"País"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.street").value("La calle es obligatoria"));
    }

    // --- Consultas ---

    @Test
    void getMyAddressesReturnsCurrentUserAddresses() throws Exception {
        User user = user(1L, "ana@example.com");
        given(ownershipGuard.currentUserId()).willReturn(1L);
        given(addressService.getByUserId(1L))
                .willReturn(List.of(address(10L, user)));

        mockMvc.perform(get("/addresses/me"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].userId").value(1));
    }

    @Test
    void getByUserReturnsList() throws Exception {
        User user = user(1L, "ana@example.com");
        given(addressService.getByUserId(1L))
                .willReturn(List.of(address(10L, user)));

        mockMvc.perform(get("/addresses/user/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].userId").value(1));
    }

    @Test
    void getByIdAndUserReturns404WhenNotOwned() throws Exception {
        given(addressService.findByIdAndUserId(99L, 1L)).willReturn(Optional.empty());

        mockMvc.perform(get("/addresses/99/user/1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail")
                        .value("No existe la dirección con id 99 para el usuario 1"));
    }

    // --- Borrado ---

    @Test
    void deleteReturns409WhenAddressHasOrders() throws Exception {
        User user = user(1L, "ana@example.com");
        given(addressService.findById(10L)).willReturn(Optional.of(address(10L, user)));
        doThrow(new IllegalStateException(
                "No se puede eliminar la dirección porque está asociada a pedidos"))
                .when(addressService).deleteById(10L);

        mockMvc.perform(delete("/addresses/10"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void deleteReturns204WhenAddressHasNoOrders() throws Exception {
        User user = user(1L, "ana@example.com");
        given(addressService.findById(10L)).willReturn(Optional.of(address(10L, user)));

        mockMvc.perform(delete("/addresses/10"))
                .andExpect(status().isNoContent());

        verify(addressService).deleteById(10L);
    }
}
