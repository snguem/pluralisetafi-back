package pluralisconseil.sn.pluralisEtatFin.helpers;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import pluralisconseil.sn.pluralisEtatFin.helpers.json.JsonModel;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class JsonService {
    private static String json_path = "public/config_complementarite_N_N_1.json";
    private static String json_sage_path = "public/config_complementarite_N_N_1_from_sage.json";

    private static ObjectMapper mapper=new ObjectMapper();

    public List<JsonModel> getAllMatrice(boolean sage) {

        try {
            if (Files.exists(Path.of(sage ? json_sage_path : json_path)))
                return mapper.readValue(
                        new File(sage ? json_sage_path : json_path),
                        new TypeReference<List<JsonModel>>(){}
                );
            else
                return null;

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<JsonModel> setMatrice(List<JsonModel> matrice_, boolean sage) {
//        mapper = new ObjectMapper();

        try {
            mapper.writeValue(new File(sage ? json_sage_path : json_path), matrice_);

            return getAllMatrice(sage);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public List<JsonModel> addMatrice(JsonModel matrice, boolean sage) {
//        mapper = new ObjectMapper();

        try {
            var matrices = getAllMatrice(sage);
            matrices.add(matrice);

            mapper.writeValue(new File(sage ? json_sage_path : json_path), matrices);

            return getAllMatrice(sage);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

}
