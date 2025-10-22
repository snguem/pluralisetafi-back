package pluralisconseil.sn.pluralisEtatFin.api.models;

import lombok.*;

import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@Getter
@Setter
public class TokenReponseDto {
    private String token;
    private String username;
    private List<String> roles=new ArrayList<String>();
}
