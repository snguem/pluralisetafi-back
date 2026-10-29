package pluralisconseil.sn.pluralisEtatFin.config;

import org.junit.jupiter.api.Test;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class JsonDatesConfigTest {

    @Test
    void datesAreSentAsIsoStringsNotArrays() throws Exception {
        var converter = new MappingJackson2HttpMessageConverter();
        List<HttpMessageConverter<?>> converters = List.of(converter);

        new JsonDatesConfig().extendMessageConverters(converters);

        assertThat(converter.getObjectMapper().writeValueAsString(LocalDate.of(2026, 10, 29))).isEqualTo("\"2026-10-29\"");
    }
}
