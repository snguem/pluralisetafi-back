package pluralisconseil.sn.pluralisEtatFin.api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import pluralisconseil.sn.pluralisEtatFin.api.models.AbonnementPlanDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.AbonnementPlan;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface AbonnementPlanMapper extends EntityMapper<AbonnementPlanDto, AbonnementPlan> {
}
