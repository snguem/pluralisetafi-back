package pluralisconseil.sn.pluralisEtatFin.api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import pluralisconseil.sn.pluralisEtatFin.api.models.ModelExcelDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ModelExcel;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface ModelExcelMapper extends EntityMapper<ModelExcelDto, ModelExcel>{

}
