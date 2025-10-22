package pluralisconseil.sn.pluralisEtatFin.api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import pluralisconseil.sn.pluralisEtatFin.api.models.ActiviteEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.DirigeantEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ActiviteEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.DirigentEntreprise;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface DirigeantEntrepriseMapper extends EntityMapper<DirigeantEntrepriseDto, DirigentEntreprise>{
    @Override
    @Mapping(source = "entrepriseId", target = "entreprise.id")
    DirigentEntreprise asEntity(DirigeantEntrepriseDto dto);

    @Override
    @Mapping(target = "entrepriseId", source = "entreprise.id")
    DirigeantEntrepriseDto asDto(DirigentEntreprise entity);
}
