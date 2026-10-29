package pluralisconseil.sn.pluralisEtatFin.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pluralisconseil.sn.pluralisEtatFin.api.controllers.DashboardRestController;
import pluralisconseil.sn.pluralisEtatFin.api.controllers.PublicRestController;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.DashboardService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserSocieteService;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The landing page endpoint needs no token; the dashboards do, and the platform one is for ADMIN_PLATEFORME. */
@WebMvcTest({PublicRestController.class, DashboardRestController.class})
@Import(SecurityConfig.class)
class PublicEndpointAccessTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean DashboardService dashboardService;
    @MockitoBean UserSocieteService userSocieteService;

    @Test
    void plansAreOpenToEveryone() throws Exception {
        when(dashboardService.publicPlans()).thenReturn(List.of());
        mockMvc.perform(get("/public/plans")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    void societeDashboardNeedsAToken() throws Exception {
        mockMvc.perform(get("/stats/societe")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/me/societe")).andExpect(status().isUnauthorized());
    }

    @Test
    void platformStatsAreForTheAdminPlateformeOnly() throws Exception {
        mockMvc.perform(get("/admin/stats").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN_COMPTE"))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/admin/stats").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN_PLATEFORME"))))
                .andExpect(status().isOk());
    }
}
