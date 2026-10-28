package pluralisconseil.sn.pluralisEtatFin.api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import pluralisconseil.sn.pluralisEtatFin.api.models.SocieteDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.Societe;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface SocieteMapper extends EntityMapper<SocieteDto, Societe> {
}
