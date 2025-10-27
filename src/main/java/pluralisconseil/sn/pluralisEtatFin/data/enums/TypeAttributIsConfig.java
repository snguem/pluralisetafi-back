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
public enum TypeAttributIsConfig {
    LIST("List"),
    STRING("Texte"),
    NUMBER("Numerique"),
    BOOLEAN("Boolean");


    private String description;

    TypeAttributIsConfig(String description) {
        this.description = description;
    }

    @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
    public static TypeAttributIsConfig fromValue(Object enu) {
        if (enu instanceof Map) {
            Map<String, Object> mapEnu = (Map<String, Object>) enu;
            if (mapEnu.containsKey("name")) {
                return TypeAttributIsConfig.valueOf(mapEnu.get("name").toString());
            }
        }
        if (enu instanceof String) {
            return TypeAttributIsConfig.valueOf(enu.toString());
        }
        throw new IllegalArgumentException(MessageFormat.format("{0} not found with the value: {1} in [{2}]", TypeAttributIsConfig.class, enu, values()));
    }

    @JsonValue
    Map<String, Object> getModule() {
        return Map.of(
                "name", name(),
                "description", description
        );
    }

    public static Set<TypeAttributIsConfig> getAllEnu() {
        return stream(values())
                .collect(Collectors.toSet());
    }
}
