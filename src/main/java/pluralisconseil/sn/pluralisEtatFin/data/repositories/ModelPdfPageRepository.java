package pluralisconseil.sn.pluralisEtatFin.data.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ModelPdfPage;

import java.util.List;
import java.util.Optional;

@Repository
public interface ModelPdfPageRepository extends JpaRepository<ModelPdfPage, Long> {
    List<ModelPdfPage> findAllByModel_IdOrderBySheetNameAsc(Long modelId);

    Optional<ModelPdfPage> findByModel_IdAndSheetNameIgnoreCase(Long modelId, String sheetName);
}
