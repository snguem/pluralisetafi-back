package pluralisconseil.sn.pluralisEtatFin.api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import pluralisconseil.sn.pluralisEtatFin.api.models.EntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Entreprise;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface EntrepriseMapper extends EntityMapper<EntrepriseDto, Entreprise>{

}
