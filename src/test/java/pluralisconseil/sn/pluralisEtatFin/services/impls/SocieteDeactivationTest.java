package pluralisconseil.sn.pluralisEtatFin.services.impls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.SocieteMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.UserSocieteMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Societe;
import pluralisconseil.sn.pluralisEtatFin.data.entities.UserSociete;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.SocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.UserSocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.exceptions.SocieteInactiveException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.SocieteMembershipNotFoundException;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.AbonnementService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.KeycloakUserService;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** Sociétés are deactivated, never deleted; a deactivated société is locked out of the API. */
@ExtendWith(MockitoExtension.class)
class SocieteDeactivationTest {

    @Mock UserSocieteRepository userSocieteRepository;
    @Mock SocieteRepository societeRepository;
    @Mock KeycloakUserService keycloakUserService;
    @Mock AbonnementService abonnementService;

    private UserSocieteServiceImpl userSocieteService() {
        return new UserSocieteServiceImpl(userSocieteRepository, new UserSocieteMapperImpl(),
                keycloakUserService, societeRepository, abonnementService);
    }

    private void member(Long societeId) {
        when(userSocieteRepository.findByKeycloakUserId("u1")).thenReturn(Optional.of(
                UserSociete.builder().keycloakUserId("u1").societeId(societeId).role("COLLABORATEUR").build()));
    }

    @Test
    void memberOfActiveSocieteIsResolved() {
        member(1L);
        when(societeRepository.findById(1L)).thenReturn(Optional.of(Societe.builder().id(1L).active(true).build()));

        assertThat(userSocieteService().resolveByKeycloakUserId("u1").getSocieteId()).isEqualTo(1L);
    }

    @Test
    void memberOfDeactivatedSocieteIsRejected() {
        member(1L);
        when(societeRepository.findById(1L)).thenReturn(Optional.of(Societe.builder().id(1L).active(false).build()));

        assertThatThrownBy(() -> userSocieteService().resolveByKeycloakUserId("u1"))
                .isInstanceOf(SocieteInactiveException.class)
                .isInstanceOf(SocieteMembershipNotFoundException.class);
    }

    @Test
    void setActiveTogglesTheFlagAndKeepsTheSociete() {
        var societe = Societe.builder().id(1L).nom("Cab").code("C").active(true).build();
        when(societeRepository.findById(1L)).thenReturn(Optional.of(societe));
        when(societeRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        var service = new SocieteServiceImpl(societeRepository, new SocieteMapperImpl(), keycloakUserService, userSocieteService());

        assertThat(service.setActive(1L, false).getActive()).isFalse();
        assertThat(service.setActive(1L, true).getActive()).isTrue();
    }
}
