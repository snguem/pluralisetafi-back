package pluralisconseil.sn.pluralisEtatFin.services.impls;

import jakarta.ws.rs.NotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.UserSocieteMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.data.entities.UserSociete;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.SocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.UserSocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.AbonnementService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.KeycloakUserService;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/** The admin of a société sees its members by name, not by Keycloak id. */
@ExtendWith(MockitoExtension.class)
class SocieteMembersTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS) Keycloak keycloak;
    @Mock UserSocieteRepository userSocieteRepository;
    @Mock KeycloakUserService keycloakUserService;
    @Mock SocieteRepository societeRepository;
    @Mock AbonnementService abonnementService;

    @Test
    void findUser_mapsTheKeycloakAccount() {
        var user = new UserRepresentation();
        user.setId("u1"); user.setUsername("fatou"); user.setEmail("f@x.sn"); user.setFirstName("Fatou"); user.setLastName("Ndiaye");
        user.setEnabled(false);
        when(keycloak.realm("pluraletatfin").users().get("u1").toRepresentation()).thenReturn(user);

        var summary = new KeycloakUserServiceImpl(keycloak, pluralisconseil.sn.pluralisEtatFin.support.TestKeycloakProperties.VALUE).findUser("u1").orElseThrow();

        assertThat(summary.username()).isEqualTo("fatou");
        assertThat(summary.enabled()).isFalse();
    }

    @Test
    void findUser_isEmptyWhenTheAccountIsGone() {
        when(keycloak.realm("pluraletatfin").users().get("gone").toRepresentation()).thenThrow(new NotFoundException());

        assertThat(new KeycloakUserServiceImpl(keycloak, pluralisconseil.sn.pluralisEtatFin.support.TestKeycloakProperties.VALUE).findUser("gone")).isEmpty();
    }

    @Test
    void listMembers_addsNamesAndSkipsAccountsGoneFromKeycloak() {
        when(userSocieteRepository.findBySocieteId(1L)).thenReturn(List.of(
                UserSociete.builder().id(10L).keycloakUserId("u1").societeId(1L).role("COLLABORATEUR").build(),
                UserSociete.builder().id(11L).keycloakUserId("gone").societeId(1L).role("INVITE").build()));
        when(keycloakUserService.findUser("u1")).thenReturn(Optional.of(new KeycloakUserService.KeycloakUserSummary(
                "u1", "fatou", "f@x.sn", "Fatou", "Ndiaye", true, List.of())));
        when(keycloakUserService.findUser("gone")).thenReturn(Optional.empty());
        var service = new UserSocieteServiceImpl(userSocieteRepository, new UserSocieteMapperImpl(), keycloakUserService,
                societeRepository, abonnementService);

        var members = service.listMembers(1L);

        assertThat(members).hasSize(1);
        assertThat(members.get(0).username()).isEqualTo("fatou");
        assertThat(members.get(0).role()).isEqualTo("COLLABORATEUR");
        assertThat(members.get(0).keycloakUserId()).isEqualTo("u1");
    }
}
