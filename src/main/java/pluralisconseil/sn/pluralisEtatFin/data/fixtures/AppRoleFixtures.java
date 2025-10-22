package pluralisconseil.sn.pluralisEtatFin.data.fixtures;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pluralisconseil.sn.pluralisEtatFin.data.entities.AppRole;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.RoleRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
@Order(1)
public class AppRoleFixtures implements CommandLineRunner {
   private final RoleRepository repository;

    @Override
    public void run(String... args) throws Exception {
        if (repository.findAll().isEmpty()){
            for (String role: List.of("Developpeur", "Administrateur", "Gestionaire")){
                repository.save(
                        AppRole.builder()
                                .roleName(role)
                                .build()
                );
            }
        }
    }
}
