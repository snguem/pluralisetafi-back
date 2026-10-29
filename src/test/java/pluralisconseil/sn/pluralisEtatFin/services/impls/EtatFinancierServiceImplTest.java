package pluralisconseil.sn.pluralisEtatFin.services.impls;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.EtatFinancierMapperImpl;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseSubstituteDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Entreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EntrepriseDatasSubstitute;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EtatFinancier;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Societe;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.EntrepriseRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.EtatFinancierRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.SocieteRepository;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Exercises {@link EtatFinancierServiceImpl} against real infrastructure, same approach as
 * {@link EntrepriseServiceImplTest}: a disposable MySQL Testcontainer stood up via the
 * {@code @DataJpaTest} slice, with {@link EtatFinancierServiceImpl} constructed by hand from
 * the slice's repositories plus a MapStruct-generated mapper impl.
 *
 * <p>Focus of this test class: the real {@code entrepriseRef} relation (a {@code @ManyToOne}
 * to {@link Entreprise}, added by Task 6) is what société-scoped queries must use — never the
 * embedded, possibly-stale {@code EntrepriseDatasSubstitute entreprise} snapshot. Most tests
 * here build their {@code EtatFinancier} rows directly via the repository (not through
 * {@code service.create(dto)}) so they can deliberately desynchronize the snapshot from the
 * real relation, exactly as a wiped/reassigned {@code Entreprise} would in production.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Testcontainers
class EtatFinancierServiceImplTest {

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
    private EtatFinancierRepository etatFinancierRepository;
    @Autowired
    private EntrepriseRepository entrepriseRepository;
    @Autowired
    private SocieteRepository societeRepository;

    private EtatFinancierServiceImpl etatFinancierService;
    private EntrepriseServiceImpl entrepriseService;

    private void setUpServices() {
        // getEntity(id) (the only EntrepriseService method EtatFinancierServiceImpl.create
        // calls) only touches entrepriseRepository, so the other EntrepriseServiceImpl
        // constructor dependencies (abonnement/dirigeant/banque/... services) are safely left
        // null for this focused test — nothing here exercises them.
        entrepriseService = new EntrepriseServiceImpl(
                entrepriseRepository, null, null, null, null, null, null, null, null);
        etatFinancierService = new EtatFinancierServiceImpl(
                etatFinancierRepository, new EtatFinancierMapperImpl(), entrepriseService);
    }

    private Entreprise newEntreprise(Societe societe, String name) {
        var substitute = new EntrepriseDatasSubstitute();
        substitute.setNameEntreprise(name);
        var entreprise = Entreprise.builder()
                .active(true)
                .substitute(substitute)
                .societe(societe)
                .build();
        return entrepriseRepository.save(entreprise);
    }

    private EtatFinancier newEtatFinancier(Entreprise entrepriseRef, Long embeddedSnapshotEntrepriseId) {
        var embeddedSnapshot = new EntrepriseDatasSubstitute();
        embeddedSnapshot.setId_(embeddedSnapshotEntrepriseId);
        var etatFinancier = EtatFinancier.builder()
                .active(true)
                .name("EF-" + System.nanoTime())
                .entreprise(embeddedSnapshot)
                .entrepriseRef(entrepriseRef)
                .build();
        return etatFinancierRepository.save(etatFinancier);
    }

    @Test
    void getAllBySociete_usesRealRelation_notEmbeddedSnapshot() {
        setUpServices();
        var societeA = societeRepository.save(Societe.builder().nom("A").active(true).build());
        var societeB = societeRepository.save(Societe.builder().nom("B").active(true).build());
        var entrepriseOfA = newEntreprise(societeA, "EntA");
        var entrepriseOfB = newEntreprise(societeB, "EntB");

        // entrepriseRef genuinely points at societe A's Entreprise, but the embedded snapshot's
        // id_ is deliberately set to societe B's Entreprise id, simulating a stale snapshot left
        // behind after an entreprise was later wiped/reassigned.
        var etatFinancier = newEtatFinancier(entrepriseOfA, entrepriseOfB.getId());

        var pageA = etatFinancierService.getAllBySociete(societeA.getId(), Map.of(), Pageable.unpaged());
        var pageB = etatFinancierService.getAllBySociete(societeB.getId(), Map.of(), Pageable.unpaged());

        assertThat(pageA.getContent()).extracting(EtatFinancierDto::getId)
                .contains(etatFinancier.getId());
        assertThat(pageB.getContent()).extracting(EtatFinancierDto::getId)
                .doesNotContain(etatFinancier.getId());
    }

    @Test
    void deleteAllBySociete_removesEveryEtatFinancierOfThatSociete_andNoOthers() {
        setUpServices();
        var societeA = societeRepository.save(Societe.builder().nom("A2").active(true).build());
        var societeB = societeRepository.save(Societe.builder().nom("B2").active(true).build());
        var entrepriseOfA = newEntreprise(societeA, "EntA2");
        var entrepriseOfB = newEntreprise(societeB, "EntB2");

        var etatFinancierOfA = newEtatFinancier(entrepriseOfA, entrepriseOfA.getId());
        var etatFinancierOfB = newEtatFinancier(entrepriseOfB, entrepriseOfB.getId());

        etatFinancierService.deleteAllBySociete(societeA.getId());

        assertThat(etatFinancierRepository.existsById(etatFinancierOfA.getId())).isFalse();
        assertThat(etatFinancierRepository.existsById(etatFinancierOfB.getId())).isTrue();
    }

    @Test
    void create_setsEntrepriseRefFromDtoEntrepriseId() {
        setUpServices();
        var societe = societeRepository.save(Societe.builder().nom("C").active(true).build());
        var entreprise = newEntreprise(societe, "EntC");

        var substituteDto = new EntrepriseSubstituteDto();
        substituteDto.setId_(entreprise.getId());
        substituteDto.setNameEntreprise("EntC");

        var dto = new EtatFinancierDto();
        dto.setName("EF-create");
        dto.setEntreprise(substituteDto);

        var created = etatFinancierService.create(dto);

        var persisted = etatFinancierRepository.findById(created.getId()).orElseThrow();
        assertThat(persisted.getEntrepriseRef()).isNotNull();
        assertThat(persisted.getEntrepriseRef().getId()).isEqualTo(entreprise.getId());
    }

    @Test
    void update_preservesEntrepriseRef_evenWhenDtoCarriesNoRelationInfo() {
        setUpServices();
        var societe = societeRepository.save(Societe.builder().nom("D").active(true).build());
        var entreprise = newEntreprise(societe, "EntD");
        var etatFinancier = newEtatFinancier(entreprise, entreprise.getId());

        var dto = new EtatFinancierDto();
        dto.setId(etatFinancier.getId());
        dto.setName("Renamed");

        etatFinancierService.update(dto);

        var persisted = etatFinancierRepository.findById(etatFinancier.getId()).orElseThrow();
        assertThat(persisted.getEntrepriseRef()).isNotNull();
        assertThat(persisted.getEntrepriseRef().getId()).isEqualTo(entreprise.getId());
        assertThat(persisted.getName()).isEqualTo("Renamed");
    }
}
