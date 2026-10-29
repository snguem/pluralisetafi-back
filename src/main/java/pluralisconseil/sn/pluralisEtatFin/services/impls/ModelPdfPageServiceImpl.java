package pluralisconseil.sn.pluralisEtatFin.services.impls;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pluralisconseil.sn.pluralisEtatFin.api.models.ModelPdfPageDto;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ModelExcel;
import pluralisconseil.sn.pluralisEtatFin.data.entities.ModelPdfPage;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.ModelExcelRepository;
import pluralisconseil.sn.pluralisEtatFin.data.repositories.ModelPdfPageRepository;
import pluralisconseil.sn.pluralisEtatFin.helpers.ExcelTemplateService;
import pluralisconseil.sn.pluralisEtatFin.helpers.LegacyPdfPageRules;
import pluralisconseil.sn.pluralisEtatFin.helpers.PdfPageSettings;
import pluralisconseil.sn.pluralisEtatFin.helpers.PrintAreas;
import pluralisconseil.sn.pluralisEtatFin.services.interfaces.ModelPdfPageService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ModelPdfPageServiceImpl implements ModelPdfPageService {

    private final ModelPdfPageRepository repository;
    private final ModelExcelRepository modelRepository;
    private final ExcelTemplateService excelTemplateService;

    @Override
    @Transactional(readOnly = true)
    public List<ModelPdfPageDto> getAll(Long modelId) {
        return repository.findAllByModel_IdOrderBySheetNameAsc(modelId).stream().map(ModelPdfPageServiceImpl::asDto).toList();
    }

    @Override
    @Transactional
    public ModelPdfPageDto create(Long modelId, ModelPdfPageDto dto) {
        var sheetName = requireSheetName(dto);
        if (repository.findByModel_IdAndSheetNameIgnoreCase(modelId, sheetName).isPresent()) {
            throw new IllegalArgumentException("La feuille « " + sheetName + " » a déjà un réglage pour ce modèle");
        }
        var entity = ModelPdfPage.builder().model(findModel(modelId)).sheetName(sheetName)
                .printArea(PrintAreas.normalize(dto.getPrintArea())).landscape(dto.isLandscape()).build();
        return asDto(repository.save(entity));
    }

    @Override
    @Transactional
    public ModelPdfPageDto update(Long modelId, Long id, ModelPdfPageDto dto) {
        var entity = findOwned(modelId, id);
        var sheetName = requireSheetName(dto);
        repository.findByModel_IdAndSheetNameIgnoreCase(modelId, sheetName)
                .filter(other -> !other.getId().equals(id))
                .ifPresent(other -> { throw new IllegalArgumentException("La feuille « " + sheetName + " » a déjà un réglage pour ce modèle"); });
        entity.setSheetName(sheetName);
        entity.setPrintArea(PrintAreas.normalize(dto.getPrintArea()));
        entity.setLandscape(dto.isLandscape());
        return asDto(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long modelId, Long id) {
        repository.delete(findOwned(modelId, id));
    }

    @Override
    @Transactional
    public ImportResult importAll(Long modelId, List<ModelPdfPageDto> dtos) {
        var model = findModel(modelId);
        // check everything first so a bad line imports nothing
        var normalized = new HashMap<String, ModelPdfPageDto>();
        for (int i = 0; i < dtos.size(); i++) {
            var dto = dtos.get(i);
            try {
                var sheetName = requireSheetName(dto);
                normalized.put(sheetName.toLowerCase(),
                        new ModelPdfPageDto(null, sheetName, PrintAreas.normalize(dto.getPrintArea()), dto.isLandscape()));
            } catch (IllegalArgumentException ex) {
                throw new IllegalArgumentException("Ligne " + (i + 1) + " : " + ex.getMessage());
            }
        }
        int created = 0, updated = 0;
        for (var dto : normalized.values()) {
            var existing = repository.findByModel_IdAndSheetNameIgnoreCase(modelId, dto.getSheetName());
            if (existing.isPresent()) {
                var entity = existing.get();
                entity.setPrintArea(dto.getPrintArea());
                entity.setLandscape(dto.isLandscape());
                repository.save(entity);
                updated++;
            } else {
                repository.save(ModelPdfPage.builder().model(model).sheetName(dto.getSheetName())
                        .printArea(dto.getPrintArea()).landscape(dto.isLandscape()).build());
                created++;
            }
        }
        return new ImportResult(created, updated);
    }

    @Override
    @Transactional
    public List<ModelPdfPageDto> seedDefaults(Long modelId) throws Exception {
        var model = findModel(modelId);
        var added = new ArrayList<ModelPdfPageDto>();
        for (var sheetName : getSheetNames(modelId)) {
            if (repository.findByModel_IdAndSheetNameIgnoreCase(modelId, sheetName).isPresent()) continue;
            var rule = LegacyPdfPageRules.forSheet(sheetName);
            added.add(asDto(repository.save(ModelPdfPage.builder().model(model).sheetName(sheetName)
                    .printArea(rule.printArea()).landscape(rule.landscape()).build())));
        }
        return added;
    }

    @Override
    public List<String> getSheetNames(Long modelId) throws Exception {
        var model = findModel(modelId);
        return excelTemplateService.getExcelPageNames(model.getExcelPath()).stream()
                .filter(name -> !name.toLowerCase().contains("evaluation warning"))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, PdfPageSettings> settingsByModel(Long modelId) {
        var settings = new HashMap<String, PdfPageSettings>();
        if (modelId == null) return settings;
        for (var page : repository.findAllByModel_IdOrderBySheetNameAsc(modelId)) {
            settings.put(page.getSheetName().toLowerCase(), new PdfPageSettings(page.getPrintArea(), page.isLandscape()));
        }
        return settings;
    }

    private ModelExcel findModel(Long modelId) {
        return modelRepository.findById(modelId).orElseThrow(() -> new IllegalArgumentException("Modèle introuvable : " + modelId));
    }

    private ModelPdfPage findOwned(Long modelId, Long id) {
        var entity = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Réglage introuvable : " + id));
        if (!entity.getModel().getId().equals(modelId)) throw new IllegalArgumentException("Ce réglage n'appartient pas à ce modèle");
        return entity;
    }

    private static String requireSheetName(ModelPdfPageDto dto) {
        if (dto == null || dto.getSheetName() == null || dto.getSheetName().isBlank()) {
            throw new IllegalArgumentException("Le nom de la feuille est obligatoire");
        }
        return dto.getSheetName().trim();
    }

    private static ModelPdfPageDto asDto(ModelPdfPage entity) {
        return new ModelPdfPageDto(entity.getId(), entity.getSheetName(), entity.getPrintArea(), entity.isLandscape());
    }
}
