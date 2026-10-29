package pluralisconseil.sn.pluralisEtatFin.services.impls;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.UserSocieteMapper;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Societe;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.SocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.UserSocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.exceptions.SocieteInactiveException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.SocieteMembershipNotFoundException;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.AbonnementService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.KeycloakUserService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/** A platform admin works in the société named by X-Societe-Id, with the rights of its ADMIN_COMPTE. */
@ExtendWith(MockitoExtension.class)
class PlatformAdminSocieteAccessTest {

    @Mock UserSocieteRepository repository;
    @Mock UserSocieteMapper mapper;
    @Mock KeycloakUserService keycloakUserService;
    @Mock SocieteRepository societeRepository;
    @Mock AbonnementService abonnementService;
    @InjectMocks UserSocieteServiceImpl service;

    @AfterEach
    void clearRequest() {
        RequestContextHolder.resetRequestAttributes();
    }

    private static void requestWithSociete(String value) {
        var request = new MockHttpServletRequest();
        if (value != null) request.addHeader("X-Societe-Id", value);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private static TestingAuthenticationToken caller(String role) {
        return new TestingAuthenticationToken("sub-1", "n/a", "ROLE_" + role);
    }

    @Test
    void platformAdminActsAsAdminCompteOfTheChosenSociete() {
        requestWithSociete("7");
        when(societeRepository.findById(7L)).thenReturn(Optional.of(Societe.builder().id(7L).active(true).build()));
        when(abonnementService.isCurrent(7L)).thenReturn(true);

        var dto = service.resolveCurrent(caller("ADMIN_PLATEFORME"));

        assertThat(dto.getSocieteId()).isEqualTo(7L);
        assertThat(dto.getRole()).isEqualTo("ADMIN_COMPTE");
        assertThat(dto.isReadOnly()).isFalse();
    }

    @Test
    void headerIsIgnoredForARegularMember() {
        requestWithSociete("7");
        lenient().when(repository.findByKeycloakUserId("sub-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveCurrent(caller("COLLABORATEUR")))
                .isInstanceOf(SocieteMembershipNotFoundException.class);
    }

    @Test
    void platformAdminWithoutHeaderHasNoSociete() {
        requestWithSociete(null);
        when(repository.findByKeycloakUserId("sub-1")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.resolveCurrent(caller("ADMIN_PLATEFORME")))
                .isInstanceOf(SocieteMembershipNotFoundException.class);
    }

    @Test
    void unknownOrDeactivatedSocieteIsRefused() {
        requestWithSociete("8");
        when(societeRepository.findById(8L)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.resolveCurrent(caller("ADMIN_PLATEFORME")))
                .isInstanceOf(SocieteMembershipNotFoundException.class);

        requestWithSociete("9");
        when(societeRepository.findById(9L)).thenReturn(Optional.of(Societe.builder().id(9L).active(false).build()));
        assertThatThrownBy(() -> service.resolveCurrent(caller("ADMIN_PLATEFORME")))
                .isInstanceOf(SocieteInactiveException.class);
    }
}
