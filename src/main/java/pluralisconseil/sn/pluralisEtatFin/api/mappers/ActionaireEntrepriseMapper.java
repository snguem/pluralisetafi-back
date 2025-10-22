package pluralisconseil.sn.pluralisEtatFin.api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import pluralisconseil.sn.pluralisEtatFin.api.models.ActionaireEntrepriseDto;
import pluralisconseil.sn.pluralisEtatFin.api.models.EtatFinancierDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ActionaireEntreprise;
import pluralisconseil.sn.pluralisEtatFin.data.entities.EtatFinancier;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface ActionaireEntrepriseMapper extends EntityMapper<ActionaireEntrepriseDto, ActionaireEntreprise>{
    @Override
    @Mapping(source = "entrepriseId", target = "entreprise.id")
    ActionaireEntreprise asEntity(ActionaireEntrepriseDto dto);

    @Override
    @Mapping(target = "entrepriseId", source = "entreprise.id")
    ActionaireEntrepriseDto asDto(ActionaireEntreprise entity);
}
