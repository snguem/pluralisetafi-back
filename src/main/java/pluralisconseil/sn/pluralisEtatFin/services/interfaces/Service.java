package pluralisconseil.sn.pluralisEtatFin.services.interfaces;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Map;

public interface Service<D> {
    D create(D dto);
    D update(D dto);
    void delete(Long id);
    D get(Long id);
    Page<D> getAll(Map<String, String> searchParams, Pageable pageable);
    long countAll();
}
