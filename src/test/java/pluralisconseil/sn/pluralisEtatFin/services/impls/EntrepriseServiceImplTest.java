package pluralisconseil.sn.pluralisEtatFin.services.impls;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.AbonnementMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.ActionaireEntrepriseMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.ActiviteEntrepriseMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.BanqueEntrepriseMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.DirigeantEntrepriseMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.EntrepriseMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.GerantEntrepriseMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.api.models.BanqueEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.DirigeantEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.AbonnementPlan;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EntrepriseDatasSubstitute;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Societe;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.AbonnementPlanRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.AbonnementRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.ActionaireEntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.ActiviteEntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.BanqueEntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.DirigeantEntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.EntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.GerantEntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.SocieteRepository;
import pluralisconseil.sn.pluralisEtatFin.exceptions.EntrepriseLockedException;
import pluralisconseil.sn.pluralisEtatFin.exceptions.EntrepriseQuotaExceededException;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Exercises {@link EntrepriseServiceImpl} against real infrastructure, same approach as
 * {@link AbonnementServiceImplTest}: a disposable MySQL Testcontainer stood up via the
 * {@code @DataJpaTest} slice (JPA/Spring Data infra only, no {@code @Service} beans), so
 * {@link EntrepriseServiceImpl} and the real {@link AbonnementServiceImpl} it depends on for
 * quota/lock checks are both constructed by hand from the slice's repositories plus
 * MapStruct-generated mapper impls.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class EntrepriseServiceImplTest {

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
    private EntrepriseRepository entrepriseRepository;
    @Autowired
    private SocieteRepository societeRepository;
    @Autowired
    private AbonnementPlanRepository planRepository;
    @Autowired
    private AbonnementRepository abonnementRepository;
    @Autowired
    private DirigeantEntrepriseRepository dirigeantEntrepriseRepository;
    @Autowired
    private BanqueEntrepriseRepository banqueEntrepriseRepository;
    @Autowired
    private ActionaireEntrepriseRepository actionaireEntrepriseRepository;
    @Autowired
    private ActiviteEntrepriseRepository activiteEntrepriseRepository;
    @Autowired
    private GerantEntrepriseRepository gerantEntrepriseRepository;
    @Autowired
    private TestEntityManager testEntityManager;

    private EntrepriseServiceImpl entrepriseService;
    private AbonnementServiceImpl abonnementService;
    private DirigeantEntrepriseServiceImpl dirigeantService;
    private BanqueEntrepriseServiceImpl banqueService;
    private ActionaireEntrepriseServiceImpl actionaireService;
    private ActiviteEntrepriseServiceImpl activiteService;
    private GerantEntrepriseServiceImpl gerantService;

    @BeforeEach
    void setUp() {
        abonnementService = new AbonnementServiceImpl(
                abonnementRepository, planRepository, societeRepository, new AbonnementMapperImpl());
        dirigeantService = new DirigeantEntrepriseServiceImpl(dirigeantEntrepriseRepository, new DirigeantEntrepriseMapperImpl());
        banqueService = new BanqueEntrepriseServiceImpl(banqueEntrepriseRepository, new BanqueEntrepriseMapperImpl());
        actionaireService = new ActionaireEntrepriseServiceImpl(actionaireEntrepriseRepository, new ActionaireEntrepriseMapperImpl());
        activiteService = new ActiviteEntrepriseServiceImpl(activiteEntrepriseRepository, new ActiviteEntrepriseMapperImpl());
        gerantService = new GerantEntrepriseServiceImpl(gerantEntrepriseRepository, new GerantEntrepriseMapperImpl());
        entrepriseService = new EntrepriseServiceImpl(
                entrepriseRepository, new EntrepriseMapperImpl(), abonnementService, planRepository,
                dirigeantService, banqueService, actionaireService, activiteService, gerantService);
    }

    private static EntrepriseDto newEntrepriseDto(String name) {
        var dto = new EntrepriseDto();
        var substitute = new EntrepriseDatasSubstitute();
        substitute.setNameEntreprise(name);
        dto.setSubstitute(substitute);
        return dto;
    }

    @Test
    void create_secondEntreprise_onSoloNonHybridePlan_throwsLocked() {
        var plan = planRepository.save(AbonnementPlan.builder().nom("SOLO")
                .entrepriseQuota(1).hybrideInclus(false).hybrideDisponibleEnOption(false)
                .prixParMois(new BigDecimal("30000")).active(true).build());
        var societe = societeRepository.save(Societe.builder().nom("A").active(true).build());
        abonnementService.activate(societe.getId(), plan.getId(), 1, LocalDate.now(), false);
        entrepriseService.create(newEntrepriseDto("First"), societe.getId());

        assertThatThrownBy(() -> entrepriseService.create(newEntrepriseDto("Second"), societe.getId()))
                .isInstanceOf(EntrepriseLockedException.class);
    }

    @Test
    void create_thirdEntreprise_onQuotaTwoPlan_throwsQuotaExceeded() {
        var plan = planRepository.save(AbonnementPlan.builder().nom("DUO")
                .entrepriseQuota(2).hybrideInclus(true).hybrideDisponibleEnOption(false)
                .prixParMois(new BigDecimal("60000")).active(true).build());
        var societe = societeRepository.save(Societe.builder().nom("B").active(true).build());
        abonnementService.activate(societe.getId(), plan.getId(), 1, LocalDate.now(), false);
        entrepriseService.create(newEntrepriseDto("First"), societe.getId());
        entrepriseService.create(newEntrepriseDto("Second"), societe.getId());

        assertThatThrownBy(() -> entrepriseService.create(newEntrepriseDto("Third"), societe.getId()))
                .isInstanceOf(EntrepriseQuotaExceededException.class);
    }

    @Test
    void create_withoutActiveAbonnement_throwsQuotaExceeded() {
        var societe = societeRepository.save(Societe.builder().nom("NoSub").active(true).build());

        assertThatThrownBy(() -> entrepriseService.create(newEntrepriseDto("X"), societe.getId()))
                .isInstanceOf(EntrepriseQuotaExceededException.class);
    }

    @Test
    void getAllBySociete_doesNotReturnOtherSocietesEntreprises() {
        var plan = planRepository.save(AbonnementPlan.builder().nom("DUO2")
                .entrepriseQuota(2).hybrideInclus(true).hybrideDisponibleEnOption(false)
                .prixParMois(new BigDecimal("60000")).active(true).build());
        var societeA = societeRepository.save(Societe.builder().nom("SocA").active(true).build());
        var societeB = societeRepository.save(Societe.builder().nom("SocB").active(true).build());
        abonnementService.activate(societeA.getId(), plan.getId(), 1, LocalDate.now(), false);
        abonnementService.activate(societeB.getId(), plan.getId(), 1, LocalDate.now(), false);
        entrepriseService.create(newEntrepriseDto("EntA"), societeA.getId());
        var entrepriseOfSocieteB = entrepriseService.create(newEntrepriseDto("EntB"), societeB.getId());

        var page = entrepriseService.getAllBySociete(societeA.getId(), Map.of(), Pageable.unpaged());

        assertThat(page.getContent()).extracting(EntrepriseDto::getId).doesNotContain(entrepriseOfSocieteB.getId());
    }

    private Long createEntrepriseWithChildren() {
        var plan = planRepository.save(AbonnementPlan.builder().nom("EXPORT")
                .entrepriseQuota(2).hybrideInclus(true).hybrideDisponibleEnOption(false)
                .prixParMois(new BigDecimal("60000")).active(true).build());
        var societe = societeRepository.save(Societe.builder().nom("ExportCo").active(true).build());
        abonnementService.activate(societe.getId(), plan.getId(), 1, LocalDate.now(), false);
        var entreprise = entrepriseService.create(newEntrepriseDto("ExportEnt"), societe.getId());

        var dirigeantDto = new DirigeantEntrepriseDto();
        dirigeantDto.setName("Jean");
        dirigeantDto.setEntrepriseId(entreprise.getId());
        dirigeantService.create(dirigeantDto);

        var banqueDto = new BanqueEntrepriseDto();
        banqueDto.setName("BanqueX");
        banqueDto.setEntrepriseId(entreprise.getId());
        banqueService.create(banqueDto);

        entreprise.getSubstitute().setCode_importeur("CODE-123");
        entreprise.getSubstitute().setRegistre_commerce("RC-123");
        entrepriseService.update(entreprise);

        // Flush and clear the shared persistence context so later export()/wipeExceptIdentity()
        // calls in this same test transaction load a genuinely fresh Entreprise from the
        // database (with Hibernate-managed, non-null @OneToMany collections) instead of reusing
        // the in-memory instance built by EntrepriseMapperImpl's Lombok-builder-based asEntity(),
        // whose collections are never initialized — matching what happens across real,
        // per-request transactions in production.
        testEntityManager.flush();
        testEntityManager.clear();

        return entreprise.getId();
    }

    @Test
    void export_returnsEntrepriseWithAllChildCollections() {
        var entrepriseId = createEntrepriseWithChildren();

        var export = entrepriseService.export(entrepriseId);

        assertThat(export.entreprise().getId()).isEqualTo(entrepriseId);
        assertThat(export.dirigeants()).hasSize(1);
        assertThat(export.banques()).hasSize(1);
        assertThat(export.actionaires()).isEmpty();
        assertThat(export.activites()).isEmpty();
        assertThat(export.gerants()).isEmpty();
    }

    @Test
    void wipeExceptIdentity_keepsNameCodeAndRegistreCommerce_deletesEverythingElse() {
        var entrepriseId = createEntrepriseWithChildren();
        var before = entrepriseService.get(entrepriseId);

        var after = entrepriseService.wipeExceptIdentity(entrepriseId);

        assertThat(after.getSubstitute().getNameEntreprise()).isEqualTo(before.getSubstitute().getNameEntreprise());
        assertThat(after.getSubstitute().getCode_importeur()).isEqualTo(before.getSubstitute().getCode_importeur());
        assertThat(after.getSubstitute().getRegistre_commerce()).isEqualTo("RC-123");
        assertThat(after.getSubstitute().getAdresse()).isNull();
        assertThat(dirigeantService.getListByEntreprise(entrepriseId)).isEmpty();
        assertThat(banqueService.getListByEntreprise(entrepriseId)).isEmpty();
    }
}
