package pluralisconseil.sn.pluralisEtatFin.api.models;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class UserLoginPasswordDto {
    private String username;
    private String password;
}
