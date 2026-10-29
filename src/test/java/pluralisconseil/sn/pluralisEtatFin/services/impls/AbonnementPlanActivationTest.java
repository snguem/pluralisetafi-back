package pluralisconseil.sn.pluralisEtatFin.services.impls;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.AbonnementPlanMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.data.entities.AbonnementPlan;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.AbonnementPlanRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/** A formula is withdrawn from the catalog, never deleted: existing abonnements keep their plan. */
@ExtendWith(MockitoExtension.class)
class AbonnementPlanActivationTest {

    @Mock AbonnementPlanRepository repository;

    @Test
    void setActive_togglesTheFlagOfThePlan() {
        when(repository.findById(1L)).thenReturn(Optional.of(AbonnementPlan.builder().id(1L).nom("Solo").active(true).build()));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));
        var service = new AbonnementPlanServiceImpl(repository, new AbonnementPlanMapperImpl());

        assertThat(service.setActive(1L, false).isActive()).isFalse();
        assertThat(service.setActive(1L, true).isActive()).isTrue();
    }
}
