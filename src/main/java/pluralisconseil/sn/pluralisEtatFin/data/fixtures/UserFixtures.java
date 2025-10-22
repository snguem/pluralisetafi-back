package pluralisconseil.sn.pluralisEtatFin.data.fixtures;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.RoleRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.UserRepository;

import java.util.List;

@Component
@RequiredArgsConstructor
@Order(2)
public class UserFixtures implements CommandLineRunner {
   private final UserRepository repository;
   private final RoleRepository roleRepository;
//   private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
//        if (repository.findByEmail("TheDev").isEmpty()){
//            User user = new User();
//            user.setEmail("TheDev");
//            user.setFullName("Nguema abessolo Steeve");
//            user.setRoles(List.of(roleRepository.findByRoleName("Developper")));
//            user.setPassword(passwordEncoder.encode("TheDevelopper23"));
//        }
    }
}
