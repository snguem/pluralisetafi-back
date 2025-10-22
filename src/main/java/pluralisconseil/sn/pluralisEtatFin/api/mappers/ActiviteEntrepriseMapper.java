package pluralisconseil.sn.pluralisEtatFin.api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import pluralisconseil.sn.pluralisEtatFin.api.models.ActiviteEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ActiviteEntreprise;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface ActiviteEntrepriseMapper extends EntityMapper<ActiviteEntrepriseDto, ActiviteEntreprise>{
    @Override
    @Mapping(source = "entrepriseId", target = "entreprise.id")
    ActiviteEntreprise asEntity(ActiviteEntrepriseDto dto);

    @Override
    @Mapping(target = "entrepriseId", source = "entreprise.id")
    ActiviteEntrepriseDto asDto(ActiviteEntreprise entity);
}
