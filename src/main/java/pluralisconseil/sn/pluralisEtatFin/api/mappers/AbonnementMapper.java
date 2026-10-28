package pluralisconseil.sn.pluralisEtatFin.api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import pluralisconseil.sn.pluralisEtatFin.api.models.AbonnementDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Abonnement;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface AbonnementMapper extends EntityMapper<AbonnementDto, Abonnement> {
}
