package pluralisconseil.sn.pluralisEtatFin.api.controllers;

import lombok.RequiredArgsConstructor;
import pluralisconseil.sn.pluralisEtatFin.api.models.GerantEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.GerantEntrepriseService;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.EntrepriseChildService;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("gerants")
@RequiredArgsConstructor
@CrossOrigin("*")
public class GerantEntrepriseRestController extends AbstractEntrepriseChildController<GerantEntrepriseDto> {

    private final GerantEntrepriseService service;

    @Override
    protected EntrepriseChildService<GerantEntrepriseDto> service() {
        return service;
    }

    @Override
    protected String entityLabel() {
        return "Gerant";
    }
}
