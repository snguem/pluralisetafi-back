package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import lombok.RequiredArgsConstructor;
import pluralisconseil.sn.pluralisEtatFin.api.models.DirigeantEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.DirigeantEntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseChildService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("dirigeants")
@RequiredArgsConstructor
@CrossOrigin("*")
public class DirigeantEntrepriseRestController extends AbstractEntrepriseChildController<DirigeantEntrepriseDto> {

    private final DirigeantEntrepriseService service;

    @Override
    protected EntrepriseChildService<DirigeantEntrepriseDto> service() {
        return service;
    }

    @Override
    protected String entityLabel() {
        return "Dirigeant";
    }
}
