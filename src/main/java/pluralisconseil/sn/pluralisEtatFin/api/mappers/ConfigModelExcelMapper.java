package pluralisconseil.sn.pluralisEtatFin.api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import pluralisconseil.sn.pluralisEtatFin.api.models.ConfigModelExcelDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ConfigModelEntrepriseField;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface ConfigModelExcelMapper extends EntityMapper<ConfigModelExcelDto, ConfigModelEntrepriseField>{

    @Override
    @Mapping(source = "model_id", target = "model.id")
    ConfigModelEntrepriseField asEntity(ConfigModelExcelDto dto);

    @Override
    @Mapping(target = "model_id", source = "model.id")
    @Mapping(target = "model_name", source = "model.name")
    ConfigModelExcelDto asDto(ConfigModelEntrepriseField entity);
}
