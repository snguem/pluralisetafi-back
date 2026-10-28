package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import lombok.RequiredArgsConstructor;
import pluralisconseil.sn.pluralisEtatFin.api.models.ActionaireEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ActionaireEntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseChildService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("actionaires")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ActionaireEntrepriseRestController extends AbstractEntrepriseChildController<ActionaireEntrepriseDto> {

    private final ActionaireEntrepriseService service;

    @Override
    protected EntrepriseChildService<ActionaireEntrepriseDto> service() {
        return service;
    }

    @Override
    protected String entityLabel() {
        return "Actionaire";
    }
}
