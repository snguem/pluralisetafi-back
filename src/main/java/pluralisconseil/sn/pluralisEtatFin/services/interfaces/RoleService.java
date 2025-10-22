package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import pluralisconseil.sn.pluralisEtatFin.api.models.RoleDto;

import java.util.List;

public interface RoleService extends Service<RoleDto> {
    List<RoleDto> getAllVisible();
}
