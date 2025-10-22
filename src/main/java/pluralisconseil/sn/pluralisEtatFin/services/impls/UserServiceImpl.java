package pluralisconseil.sn.pluralisEtatFin.services.impls;

import com.querydsl.core.BooleanBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.api.mappers.UserMapper;
import pluralisconseil.sn.pluralisEtatFin.api.models.LoginDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.AppRole;
import pluralisconseil.sn.pluralisEtatFin.data.entities.QUser;
import pluralisconseil.sn.pluralisEtatFin.data.entities.User;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.RoleRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.UserRepository;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.UserService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService, UserDetailsService {
    private final UserRepository repository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper mapper;



    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User appUser = repository.findByLogin(username).get();
        if (appUser == null) throw new RuntimeException("User not found ");
        List<SimpleGrantedAuthority> authorities = appUser.getRoles()
                .stream()
                .map(appRole -> new SimpleGrantedAuthority(appRole.getRoleName())).toList();
        return new org.springframework.security.core.userdetails.User(appUser.getUsername(), appUser.getPassword(),authorities);
    }

    @Override
    public UserDto getByLogin(String login) {
        var entity = repository.findByLogin(login);
        UserDto dto = null;
        if (entity.isPresent())
            dto = mapper.asDto(entity.get());
        return dto;
    }

    //    @Override
//    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
//        User user = repository.findByLogin(username).get();
//        if (user == null) throw new UsernameNotFoundException("User not found");
//        return new org.springframework.security.core.userdetails.User(
//                user.getLogin(),
//                user.getPassword(),
//                user.getRoles().stream()
//                        .map(role -> new SimpleGrantedAuthority(role.getRoleName()))
//                        .collect(Collectors.toList())
//        );
//    }
//
    @Override
    public UserDto create(UserDto dto) {
        var entity = mapper.asEntity(dto);
        entity.setActive(true);
        entity.setPassword(passwordEncoder.encode(entity.getPassword()));
        entity.setRoles(new ArrayList<>());
        for (String role:dto.getRoles_string()){
            var role_entity=roleRepository.findByRoleName(role);
            if (role_entity!=null)
                entity.getRoles().add(role_entity);
        }
        if (entity.getRoles().isEmpty())
            return null;
        var entitySave = repository.save(entity);
        return mapper.asDto(entitySave);
    }

    @Override
    public UserDto update(UserDto dto) {
        var entity = mapper.asEntity(dto);
        var entitySave = repository.save(entity);
        return mapper.asDto(entitySave);
    }

    @Override
    public void delete(Long id) {
        if(!repository.existsById(id)) {
            throw new RuntimeException("User not found");
        }
        repository.deleteById(id);
    }

    @Override
    public UserDto get(Long id) {
        var entity = repository.findById(id);
        return mapper.asDto(entity.get());
    }

    @Override
    public Page<UserDto> getAll(Map<String, String> searchParams, Pageable pageable) {
        var booleanBuilder = new BooleanBuilder();
        var q= QUser.user;
        booleanBuilder.and(q.roles.any().roleName.notEqualsIgnoreCase("Developpeur"));
        buildSearch(searchParams, booleanBuilder);
        var dtos = repository.findAll(booleanBuilder, pageable)
                .map(mapper::asDto);
        dtos.forEach((us)->{
            us.setRoles_string(repository.findById(us.getId()).get().getRoles().stream().map(AppRole::getRoleName).collect(Collectors.toList()));
        });
        return dtos;
    }



    private void buildSearch(Map<String, String> searchParams, BooleanBuilder booleanBuilder) {
        if (Objects.nonNull(searchParams)) {
//            var qEntity = QUser.user;
        }
    }
}
