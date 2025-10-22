package pluralisconseil.sn.pluralisEtatFin.api.models;

import lombok.Data;

@Data
public class LoginDto {
    private String email;

    private String password;
}