package pluralisconseil.sn.pluralisEtatFin.data.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;

import java.text.MessageFormat;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;


@Getter
public enum TypeConfig {
    ENTREPRISE("Entreprise"),
    ETATFINANCIER("Etat financier");


    private String description;

    TypeConfig(String description) {
        this.description = description;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TypeConfig fromValue(Object enu) {
        if (enu instanceof Map) {
            Map<String, Object> mapEnu = (Map<String, Object>) enu;
            if (mapEnu.containsKey("name")) {
                return TypeConfig.valueOf(mapEnu.get("name").toString());
            }
        }
        if (enu instanceof String) {
            return TypeConfig.valueOf(enu.toString());
        }
        throw new IllegalArgumentException(MessageFormat.format("{0} not found with the value: {1} in [{2}]", TypeConfig.class, enu, values()));
    }

    @JsonValue
    Map<String, Object> getModule() {
        return Map.of(
                "name", name(),
                "description", description
        );
    }

    public static Set<TypeConfig> getAllEnu() {
        return stream(values())
                .collect(Collectors.toSet());
    }
}
