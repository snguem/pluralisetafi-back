package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import lombok.RequiredArgsConstructor;
import pluralisconseil.sn.pluralisEtatFin.api.models.ActiviteEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ActiviteEntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseChildService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("activites")
@RequiredArgsConstructor
@CrossOrigin("*")
public class ActiviteEntrepriseRestController extends AbstractEntrepriseChildController<ActiviteEntrepriseDto> {

    private final ActiviteEntrepriseService service;

    @Override
    protected EntrepriseChildService<ActiviteEntrepriseDto> service() {
        return service;
    }

    @Override
    protected String entityLabel() {
        return "Activite";
    }
}
