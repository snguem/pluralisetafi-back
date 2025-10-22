package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pluralisconseil.sn.pluralisEtatFin.api.models.ConfigModelExcelDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ModelExcel;

import java.util.List;
import java.util.Map;

public interface ConfigModelExcelService extends Service<ConfigModelExcelDto> {
    Page<ConfigModelExcelDto> getAllByModelExcel(long model_id, Map<String, String> searchParams, Pageable pageable);
    List<ConfigModelExcelDto> getAll(Map<String, String> searchParams);
}
