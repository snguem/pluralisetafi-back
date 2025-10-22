package pluralisconseil.sn.pluralisEtatFin.api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import pluralisconseil.sn.pluralisEtatFin.api.models.DirigeantEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.GerantEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.DirigentEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.GerantEntreprise;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface GerantEntrepriseMapper extends EntityMapper<GerantEntrepriseDto, GerantEntreprise>{
    @Override
    @Mapping(source = "entrepriseId", target = "entreprise.id")
    GerantEntreprise asEntity(GerantEntrepriseDto dto);

    @Override
    @Mapping(target = "entrepriseId", source = "entreprise.id")
    GerantEntrepriseDto asDto(GerantEntreprise entity);
}
