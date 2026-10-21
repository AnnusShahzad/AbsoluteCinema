package com.absolutecinema.config;

import com.absolutecinema.controller.AccountController;
import com.absolutecinema.controller.AdminController;
import com.absolutecinema.controller.AuthController;
import com.absolutecinema.controller.BookingController;
import com.absolutecinema.controller.LoginSuccessHandler;
import com.absolutecinema.controller.SnackAPIController;
import com.absolutecinema.entity.Snack;
import com.absolutecinema.repository.UserRepository;
import com.absolutecinema.service.AnalyticsService;
import com.absolutecinema.service.BookingService;
import com.absolutecinema.service.ContactService;
import com.absolutecinema.service.LoyaltyService;
import com.absolutecinema.service.MovieService;
import com.absolutecinema.service.PasswordResetService;
import com.absolutecinema.service.SnackService;
import com.absolutecinema.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = { AdminController.class, BookingController.class, SnackAPIController.class,
        AuthController.class, AccountController.class })
@Import({ SecurityConfig.class, LoginSuccessHandler.class })
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private UserService userService;
    @MockitoBean
    private MovieService movieService;
    @MockitoBean
    private SnackService snackService;
    @MockitoBean
    private BookingService bookingService;
    @MockitoBean
    private LoyaltyService loyaltyService;
    @MockitoBean
    private AnalyticsService analyticsService;
    @MockitoBean
    private ContactService contactService;
    @MockitoBean
    private PasswordResetService passwordResetService;

    @Test
    void accountTypesMapToRoles() {
        assertThat(SecurityConfig.roleFor("admin")).isEqualTo("ROLE_ADMIN");
        assertThat(SecurityConfig.roleFor("Employee")).isEqualTo("ROLE_EMPLOYEE");
        assertThat(SecurityConfig.roleFor("user")).isEqualTo("ROLE_USER");
        assertThat(SecurityConfig.roleFor(null)).isEqualTo("ROLE_USER");
    }

    @Test
    void anonymousVisitorsAreSentToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    void customersCannotOpenAdminPages() throws Exception {
        mockMvc.perform(get("/admin/managebooking").with(user("demo").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void customersCanReadButNotChangeTheCatalog() throws Exception {
        when(snackService.findAll()).thenReturn(List.of(new Snack()));

        mockMvc.perform(get("/api/snacks").with(user("demo").roles("USER")))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/snacks/1").with(user("demo").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void customersCannotCancelOtherBookings() throws Exception {
        mockMvc.perform(post("/bookings/1/cancel").with(user("demo").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminsCanDeleteSnacksAndCancelBookings() throws Exception {
        when(snackService.findById(1L)).thenReturn(Optional.of(new Snack()));

        mockMvc.perform(delete("/api/snacks/1").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/bookings/7/cancel").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNoContent());
        verify(snackService).deleteById(1L);
        verify(bookingService).cancelBooking(7L);
    }

    @Test
    void stateChangingRequestsRequireCsrfToken() throws Exception {
        mockMvc.perform(delete("/api/snacks/1").with(user("admin").roles("ADMIN")))
                .andExpect(status().isForbidden());
    }

    @Test
    void passwordResetIsAvailableWithoutSigningIn() throws Exception {
        mockMvc.perform(post("/auth/forgot-password/code").with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"demo@example.com\"}"))
                .andExpect(status().isOk());
        verify(passwordResetService).requestCode("demo@example.com");
    }

    @Test
    void accountSettingsRequireSigningIn() throws Exception {
        mockMvc.perform(get("/account/settings"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/auth/login"));
    }

    @Test
    void onlyAdminsCanReadOrDeleteContactMessages() throws Exception {
        mockMvc.perform(get("/admin/messages").with(user("demo").roles("USER")))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete("/admin/messages/3").with(user("demo").roles("USER")).with(csrf()))
                .andExpect(status().isForbidden());

        when(contactService.delete(3L)).thenReturn(true);
        mockMvc.perform(delete("/admin/messages/3").with(user("admin").roles("ADMIN")).with(csrf()))
                .andExpect(status().isNoContent());
    }

    @Test
    void employeesCannotOpenAdminPages() throws Exception {
        mockMvc.perform(get("/admin/analytics").with(user("staff").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }
}
