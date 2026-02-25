package pluralisconseil.sn.pluralisEtatFin.config;

import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JodConverterConfig {
    @Value("${jodconverter.local.office-home}")
    private String officeHome;

    @Bean(initMethod = "start", destroyMethod = "stop")
    public OfficeManager officeManager() {
        LocalOfficeManager manager = LocalOfficeManager.builder()
                .officeHome(officeHome)
                .install()
                .build();
        return manager;
    }

    @Bean
    public DocumentConverter documentConverter(OfficeManager officeManager) {
        return LocalConverter.make(officeManager);
    }
}
