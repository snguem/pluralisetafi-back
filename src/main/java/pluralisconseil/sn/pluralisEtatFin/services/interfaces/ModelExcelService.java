package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import pluralisconseil.sn.pluralisEtatFin.api.models.ModelExcelDto;

import java.util.List;

public interface ModelExcelService extends Service<ModelExcelDto> {
    ModelExcelDto getName(String name);

}
