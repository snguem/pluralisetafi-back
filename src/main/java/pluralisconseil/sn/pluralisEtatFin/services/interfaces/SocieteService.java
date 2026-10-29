package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import pluralisconseil.sn.pluralisEtatFin.api.models.SocieteDto;

import java.util.Map;

/**
 * Creates and reads {@code Societe} tenants. {@link #create} also provisions the société's
 * first admin account directly in Keycloak and links it to the new société via
 * {@link UserSocieteService#attach}.
 */
public interface SocieteService {

    SocieteDto create(String nom, String adminUsername, String adminEmail,
                       String adminFirstName, String adminLastName, String adminTemporaryPassword);

    SocieteDto get(Long id);

    /**
     * Activates or deactivates a société (sociétés are never deleted). While deactivated, none of
     * its members can use the API: see {@link UserSocieteService#resolveCurrent}.
     */
    SocieteDto setActive(Long id, boolean active);

    Page<SocieteDto> getAll(Map<String, String> searchParams, Pageable pageable);
}
