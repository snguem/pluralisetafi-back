package pluralisconseil.sn.pluralisEtatFin.schedule;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import pluralisconseil.sn.pluralisEtatFin.helpers.HelperService;


@Component
@EnableScheduling
@RequiredArgsConstructor
public class TempFileShedule {
    private final HelperService service;
//    @Scheduled(cron = "0 10 0 * * *")
    @Scheduled(cron = "0 0 20 * * *") // Tous les jours à 20h00 (heure locale du serveur)
    @Transactional
    public void controleDateStatut() {
        service.deleteFilesInDirectory("public/temps");
    }
}
