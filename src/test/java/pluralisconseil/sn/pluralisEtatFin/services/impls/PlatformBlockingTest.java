package pluralisconseil.sn.pluralisEtatFin.services.impls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.AbonnementMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Societe;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.AbonnementPlanRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.AbonnementRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.SocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.exceptions.SocieteInactiveException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** The platform admin can block a deactivated société out of subscriptions, and block/unblock user accounts. */
@ExtendWith(MockitoExtension.class)
class PlatformBlockingTest {

    @Mock AbonnementRepository abonnementRepository;
    @Mock AbonnementPlanRepository planRepository;
    @Mock SocieteRepository societeRepository;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS) Keycloak keycloak;

    @Test
    void abonnementCannotBeActivatedOnDeactivatedSociete() {
        when(societeRepository.findById(1L)).thenReturn(Optional.of(Societe.builder().id(1L).active(false).build()));
        var service = new AbonnementServiceImpl(abonnementRepository, planRepository, societeRepository, new AbonnementMapperImpl());

        assertThatThrownBy(() -> service.activate(1L, 1L, 1, LocalDate.now(), false))
                .isInstanceOf(SocieteInactiveException.class);
        verify(abonnementRepository, never()).save(any());
    }

    @Test
    void blockingAUserDisablesItInKeycloakAndEndsItsSessions() {
        var user = new UserRepresentation();
        user.setEnabled(true);
        var resource = keycloak.realm("pluraletatfin").users().get("u1");
        when(resource.toRepresentation()).thenReturn(user);

        new KeycloakUserServiceImpl(keycloak).setUserEnabled("u1", false);

        assertThat(user.isEnabled()).isFalse();
        verify(resource).update(user);
        verify(resource).logout();
    }

    @Test
    void unblockingAUserEnablesItWithoutTouchingSessions() {
        var user = new UserRepresentation();
        user.setEnabled(false);
        var resource = keycloak.realm("pluraletatfin").users().get("u1");
        when(resource.toRepresentation()).thenReturn(user);

        new KeycloakUserServiceImpl(keycloak).setUserEnabled("u1", true);

        assertThat(user.isEnabled()).isTrue();
        verify(resource).update(user);
        verify(resource, never()).logout();
    }
}
