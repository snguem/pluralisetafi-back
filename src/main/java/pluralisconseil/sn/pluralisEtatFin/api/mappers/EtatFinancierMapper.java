package pluralisconseil.sn.pluralisEtatFin.api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EtatFinancier;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface EtatFinancierMapper extends EntityMapper<EtatFinancierDto, EtatFinancier>{
    @Override
    @Mapping(source = "entrepriseId", target = "entreprise.id")
    @Mapping(source = "modelExcel_id", target = "model.id")
    EtatFinancier asEntity(EtatFinancierDto dto);

    @Override
    @Mapping(target = "entrepriseId", source = "entreprise.id")
    @Mapping(target = "modelExcel_id", source = "model.id")
    @Mapping(target = "entreprise_name", source = "entreprise.name")
    @Mapping(target = "modelExcel_name", source = "model.name")
    EtatFinancierDto asDto(EtatFinancier entity);
}
