package pluralisconseil.sn.pluralisEtatFin.helpers.json;

import lombok.Data;

import java.util.List;


@Data
public class JsonModel {
    private String pageN;
    private String pageN_1;

    private List<JsonMatriceModel> matrice;
}
