package pluralisconseil.sn.pluralisEtatFin.api.mappers;

import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;
import pluralisconseil.sn.pluralisEtatFin.api.models.UserDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.User;

@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE, componentModel = "spring")
public interface UserMapper extends EntityMapper<UserDto, User>{

}
