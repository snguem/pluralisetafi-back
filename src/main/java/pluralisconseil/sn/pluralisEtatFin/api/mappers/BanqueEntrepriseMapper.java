package pluralisconseil.sn.pluralisEtatFin.api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import pluralisconseil.sn.pluralisEtatFin.api.models.BanqueEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.GerantEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.BanqueEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.GerantEntreprise;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface BanqueEntrepriseMapper extends EntityMapper<BanqueEntrepriseDto, BanqueEntreprise>{
    @Override
    @Mapping(source = "entrepriseId", target = "entreprise.id")
    BanqueEntreprise asEntity(BanqueEntrepriseDto dto);

    @Override
    @Mapping(target = "entrepriseId", source = "entreprise.id")
    BanqueEntrepriseDto asDto(BanqueEntreprise entity);
}
