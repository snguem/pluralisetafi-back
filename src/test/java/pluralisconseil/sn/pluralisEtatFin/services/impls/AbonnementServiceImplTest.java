package pluralisconseil.sn.pluralisEtatFin.services.impls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.AbonnementMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.data.entities.AbonnementPlan;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Societe;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.AbonnementPlanRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.AbonnementRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.SocieteRepository;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link AbonnementServiceImpl} against real infrastructure: a real
 * {@code AbonnementPlan}/{@code Abonnement}/{@code Societe} persistence layer (a disposable
 * MySQL Testcontainer, local to this test class, stood up via the {@code @DataJpaTest} slice
 * with {@code replace = NONE} so it is not swapped for an embedded database), same approach as
 * {@link SocieteServiceImplTest}.
 *
 * <p>The {@code @DataJpaTest} slice only loads JPA infrastructure (entities, Spring Data
 * repositories), not {@code @Service} beans, so {@link AbonnementServiceImpl} is constructed by
 * hand from the slice's real, Spring-Data-backed repositories plus a MapStruct-generated mapper
 * impl.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class AbonnementServiceImplTest {

    @Container
    static final GenericContainer<?> MYSQL = new GenericContainer<>(DockerImageName.parse("mysql:8.4"))
            .withEnv("MYSQL_ROOT_PASSWORD", "test")
            .withEnv("MYSQL_DATABASE", "etat_fin_app")
            .withExposedPorts(3306)
            .waitingFor(Wait.forLogMessage(".*ready for connections.*\\n", 2)
                    .withStartupTimeout(Duration.ofMinutes(2)));

    @DynamicPropertySource
    static void datasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () ->
                "jdbc:mysql://" + MYSQL.getHost() + ":" + MYSQL.getMappedPort(3306) + "/etat_fin_app");
        registry.add("spring.datasource.username", () -> "root");
        registry.add("spring.datasource.password", () -> "test");
    }

    @Autowired
    private SocieteRepository societeRepository;
    @Autowired
    private AbonnementPlanRepository planRepository;
    @Autowired
    private AbonnementRepository abonnementRepository;

    private AbonnementServiceImpl abonnementService;

    @BeforeEach
    void setUp() {
        abonnementService = new AbonnementServiceImpl(
                abonnementRepository, planRepository, societeRepository, new AbonnementMapperImpl());
    }

    @Test
    void activate_setsHybrideFromPlanWhenIncluded() {
        var plan = planRepository.save(AbonnementPlan.builder().nom("SOLO_HYBRIDE")
                .entrepriseQuota(1).hybrideInclus(true).hybrideDisponibleEnOption(false)
                .prixParMois(new BigDecimal("50000")).active(true).build());
        var societe = societeRepository.save(Societe.builder().nom("X").active(true).build());

        var dto = abonnementService.activate(societe.getId(), plan.getId(), 1, LocalDate.now(), false);

        assertThat(dto.isHybride()).isTrue();
        assertThat(dto.getDateFin()).isEqualTo(LocalDate.now().plusMonths(1));
    }

    @Test
    void activate_usesCallerChoiceWhenHybrideIsOptional() {
        var plan = planRepository.save(AbonnementPlan.builder().nom("PACK_OPTION_HYBRIDE")
                .entrepriseQuota(3).hybrideInclus(false).hybrideDisponibleEnOption(true)
                .hybrideSupplementPrix(new BigDecimal("10000"))
                .prixParMois(new BigDecimal("75000")).active(true).build());
        var societe = societeRepository.save(Societe.builder().nom("Y").active(true).build());

        var withHybride = abonnementService.activate(societe.getId(), plan.getId(), 2, LocalDate.now(), true);

        assertThat(withHybride.isHybride()).isTrue();
        assertThat(withHybride.getDateFin()).isEqualTo(LocalDate.now().plusMonths(2));
    }

    @Test
    void activate_deactivatesPreviousAbonnementForSameSociete() {
        var plan = planRepository.save(AbonnementPlan.builder().nom("SOLO")
                .entrepriseQuota(1).hybrideInclus(false).hybrideDisponibleEnOption(false)
                .prixParMois(new BigDecimal("30000")).active(true).build());
        var societe = societeRepository.save(Societe.builder().nom("Z").active(true).build());

        var first = abonnementService.activate(societe.getId(), plan.getId(), 1, LocalDate.now(), false);
        var second = abonnementService.activate(societe.getId(), plan.getId(), 1, LocalDate.now(), false);

        assertThat(abonnementRepository.findById(first.getId()).orElseThrow().isActive()).isFalse();
        assertThat(abonnementRepository.findById(second.getId()).orElseThrow().isActive()).isTrue();
        assertThat(abonnementService.getActiveEntity(societe.getId()).getId()).isEqualTo(second.getId());
    }

    @Test
    void getActiveEntity_returnsNullWhenNoneActive() {
        var societe = societeRepository.save(Societe.builder().nom("W").active(true).build());

        assertThat(abonnementService.getActiveEntity(societe.getId())).isNull();
    }
}
