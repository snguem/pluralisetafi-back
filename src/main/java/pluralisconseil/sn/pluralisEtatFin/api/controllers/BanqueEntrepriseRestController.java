package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import lombok.RequiredArgsConstructor;
import pluralisconseil.sn.pluralisEtatFin.api.models.BanqueEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.BanqueEntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseChildService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("banques")
@RequiredArgsConstructor
@CrossOrigin("*")
public class BanqueEntrepriseRestController extends AbstractEntrepriseChildController<BanqueEntrepriseDto> {

    private final BanqueEntrepriseService service;

    @Override
    protected EntrepriseChildService<BanqueEntrepriseDto> service() {
        return service;
    }

    @Override
    protected String entityLabel() {
        return "Banque";
    }
}
