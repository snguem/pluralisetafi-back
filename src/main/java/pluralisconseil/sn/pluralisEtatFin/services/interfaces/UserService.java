package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import org.springframework.security.core.userdetails.UserDetailsService;
import pluralisconseil.sn.pluralisEtatFin.api.models.LoginDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.User;

import java.util.Map;

public interface UserService extends Service<UserDto> {
//    User authenticate(LoginDto input);

    int updateBasic(UserDto dto);
    UserDto getByLogin(String login);
    UserDto login(LoginDto loginDto);

    int updateNewPassword(String username, String password);
}
