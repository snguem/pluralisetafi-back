package pluralisconseil.sn.pluralisEtatFin.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import pluralisconseil.sn.pluralisEtatFin.api.models.ModelExcelDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import pluralisconseil.sn.pluralisEtatFin.api.controllers.ModelExcelRestController;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.ModelExcelMapper;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelTemplateService;
import pluralisconseil.sn.pluralisEtatFin.helpers.HelperService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ModelExcelService;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Models (and their config) are global: only ADMIN_PLATEFORME may write them, every société may read them. */
@WebMvcTest(ModelExcelRestController.class)
@Import(SecurityConfig.class)
class ModelsWriteAccessTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean JwtDecoder jwtDecoder;
    @MockitoBean ModelExcelService modelExcelService;
    @MockitoBean ExcelTemplateService excelTemplateService;
    @MockitoBean HelperService helperService;
    @MockitoBean ModelExcelMapper mapper;

    @BeforeEach
    void stubService() {
        when(modelExcelService.getAll(any(), any())).thenReturn(Page.empty());
        when(modelExcelService.get(1L)).thenReturn(new ModelExcelDto());
    }

    @Test
    void aSocieteAdminCannotDeleteAModel() throws Exception {
        mockMvc.perform(delete("/models/1").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN_COMPTE"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void aCollaborateurCannotDeleteAModelNorItsConfig() throws Exception {
        var collaborateur = jwt().authorities(new SimpleGrantedAuthority("ROLE_COLLABORATEUR"));
        mockMvc.perform(delete("/models/1").with(collaborateur)).andExpect(status().isForbidden());
        mockMvc.perform(delete("/config-model-excel/1").with(collaborateur)).andExpect(status().isForbidden());
    }

    @Test
    void aCollaborateurCanStillReadModels() throws Exception {
        mockMvc.perform(get("/models/all").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_COLLABORATEUR"))))
                .andExpect(status().is(org.hamcrest.Matchers.not(403)));
    }

    @Test
    void theAdminPlateformeCanWriteModels() throws Exception {
        mockMvc.perform(delete("/models/1").with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ADMIN_PLATEFORME"))))
                .andExpect(status().is(org.hamcrest.Matchers.not(403)));
    }
}
