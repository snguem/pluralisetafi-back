# Refactor architecture (MVC / SOLID, décomposition des god classes) Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Décomposer `ExcelService` et `EtatFinancierRestController` (les deux god classes du
pipeline d'import de balance) en classes à responsabilité unique, derrière des interfaces,
sans changer le comportement observable — c'est la première des quatre fondations
(refactor → Keycloak → multi-tenant → import simplifié).

**Architecture:** Extraction progressive, une responsabilité à la fois, chaque extraction
protégée par un test avant d'être déplacée. Les méthodes les plus simples (`ExcelTemplateService`,
`ExcelCleanupService`) sont testées en TDD classique avec des classeurs Apache POI construits en
mémoire. Les méthodes les plus complexes et les plus couplées (`configExcelFile`, `complement`)
sont déplacées telles quelles (aucune réécriture), avec un test de non-régression qui documente
leur comportement actuel — y compris ses défauts, qui ne sont pas corrigés dans ce plan (voir
Review Focus).

**Tech Stack:** Java 21, Spring Boot 3.5.6, Apache POI (`poi-ooxml`), JODConverter (LibreOffice)
(`jodconverter`), JUnit 5 + Mockito + AssertJ (`spring-boot-starter-test`, déjà en
dépendance).

**Spec:** [docs/superpowers/specs/2026-10-22-evolution-multi-tenant-keycloak-design.md](../specs/2026-10-22-evolution-multi-tenant-keycloak-design.md)
(section 4.5 "Refactor : MVC / SOLID / segmentation des god classes")

## Global Constraints

- Java 21 / Spring Boot 3.5.6 : pas de changement de version dans ce plan.
- Aucune donnée existante à préserver, mais le **comportement** du pipeline d'import actuel
  doit rester identique à l'issue de ce plan — la simplification de l'import (remplacement de
  `complement`/Sage par le calcul `GAUCHE`) est un plan séparé, plus tard dans l'ordre convenu.
- Toute nouvelle classe de service suit la convention déjà établie dans le projet : une
  interface + une implémentation `*Impl` + injection par constructeur (Lombok
  `@RequiredArgsConstructor`), jamais de classe concrète injectée directement.
- Pas de suppression de fonctionnalité : chaque endpoint `/etats-fin/**` existant doit répondre
  exactement comme avant à l'issue de ce plan.

## Review Focus

- **Balance de plus de 200 lignes.** `addBalanceToModel` boucle `for (row = 1; row < 200; ...)` :
  toute ligne de balance au-delà de la 200e est silencieusement ignorée. Pas un bug introduit
  par ce refactor, mais le test de caractérisation doit documenter cette limite explicitement
  pour qu'elle ne soit ni "corrigée" par erreur ici, ni oubliée pour le plan d'import simplifié.
- **Feuille "BAL N" absente du modèle.** Si `workbook.getSheet(sheetModel)` renvoie `null`,
  l'appel suivant (`balN.getRow(...)`) lève une `NullPointerException` opaque plutôt qu'une
  erreur explicite. Le test de caractérisation doit figer ce comportement actuel (ou, à défaut,
  documenter clairement qu'il s'agit d'une `NullPointerException` attendue à ce stade).
- **`complement(boolean, String, String)` (3 arguments) écrit sur un chemin figé, pas sur le
  fichier de l'état financier.** Trouvé en lisant le code : cette surcharge écrit toujours dans
  `"public/dossier/test2.xlsx"`, jamais dans `etatFinNpath`. Or c'est **cette** surcharge qui est
  appelée en production, dans `EtatFinancierRestController.setBalancesWithEtatFinN1` (ligne 147 :
  `excelService.complement(false, dto.getExcelPath(), etat_fin_n_1)`). Autrement dit, le
  "complément depuis l'état financier N-1" ne modifie jamais le fichier réellement utilisé par
  l'utilisateur — probable bug de production existant, signalé au porteur du projet en dehors de
  ce document. Ce plan **préserve ce comportement tel quel** (aucune réécriture n'est dans son
  périmètre) ; le test de caractérisation doit figer ce chemin en dur pour que la bizarrerie soit
  visible et traçable, pas corrigée en silence pendant une extraction mécanique.
- **Types de cellule non prévus par le `switch`.** `copyCellValue`/`addBalanceToModel` couvrent
  `STRING`/`NUMERIC`/`BOOLEAN`/`FORMULA` ; toute autre valeur (ex. `BLANK` dans
  `addBalanceToModel`, pas dans `copyCellValue`) tombe dans la branche `default`. Le test de
  caractérisation doit couvrir au moins une cellule de chaque type présent dans la branche par
  défaut pour ne pas introduire de régression silencieuse lors du déplacement.
- **Erreur de réflexion dans `configExcelFile`.** Si le nom de champ d'un `ConfigModelExcelDto`
  ne correspond à aucun getter du DTO ciblé, `getMethod(...)` lève une `NoSuchMethodException`
  checked, remontée telle quelle par la signature `throws Exception`. Le test de
  non-régression (smoke test, pas couverture exhaustive) doit au moins vérifier qu'un cas nominal
  (un champ existant) fonctionne après déplacement, sans prétendre couvrir les 6 branches de
  `TypeAttributIsConfig`.

---

## Task 1: Test de caractérisation — `ExcelService.fuseBalanceToExcel` (1 balance)

Fige le comportement actuel avant toute extraction. Ce test sera déplacé (pas réécrit) dans la
Task 4 une fois `BalanceMergeService` créé.

**Files:**
- Create: `src/test/java/pluralisconseil/sn/pluralisEtatFin/helpers/ExcelServiceCharacterizationTest.java`
- Create: `src/test/java/pluralisconseil/sn/pluralisEtatFin/helpers/support/WorkbookTestFixtures.java` (fabrique de classeurs POI en mémoire, réutilisée par les tasks suivantes)

**Interfaces:**
- Consumes: `ExcelService.fuseBalanceToExcel(EtatFinancierDto dto, String model_path, String balance_n_path)` (signature actuelle, `helpers/ExcelService.java:113`)
- Produces: `WorkbookTestFixtures.modelWithSheet(String sheetName, int rowCount)` et
  `WorkbookTestFixtures.balanceWorkbook(List<Object[]> rows)` — utilitaires réutilisés par les
  Tasks 2, 3 et 4 pour construire des classeurs `.xlsx` temporaires sans dépendre des fichiers
  binaires existants dans `public/excels/`.

- [ ] **Step 1: Écrire `WorkbookTestFixtures`**

```java
public final class WorkbookTestFixtures {
    // Crée un classeur avec une feuille nommée `sheetName`, `rowCount` lignes vides
    // (colonnes A à H), écrit dans un fichier temporaire, retourne son chemin absolu.
    public static String modelWithSheet(String sheetName, int rowCount) throws IOException { ... }

    // Crée un classeur à une seule feuille dont la ligne 0 est un en-tête et les lignes
    // suivantes contiennent `rows` (une Object[] par ligne, colonnes A..), écrit dans un
    // fichier temporaire, retourne son chemin absolu.
    public static String balanceWorkbook(List<Object[]> rows) throws IOException { ... }
}
```

Laisser l'implémentation (ouverture `XSSFWorkbook`, écriture `FileOutputStream` dans
`Files.createTempFile(...)`) au développeur — aucune règle métier dedans, seulement de la
construction de fixture.

- [ ] **Step 2: Écrire le test de caractérisation**

```java
class ExcelServiceCharacterizationTest {
    @Test
    void fuseBalanceToExcel_copiesAccountRowsIntoBalNSheet() throws Exception {
        String modelPath = WorkbookTestFixtures.modelWithSheet("BAL N", 10);
        String balancePath = WorkbookTestFixtures.balanceWorkbook(List.of(
            new Object[]{"601000", "Achats", 150000.0},
            new Object[]{"701000", "Ventes", -300000.0}
        ));
        EtatFinancierDto dto = EtatFinancierDto.builder().id(1L)
            .entreprise(EntrepriseSubstituteDto.builder().nameEntreprise("Acme").build())
            .annee_n(2025).build();

        EtatFinancierDto result = excelService.fuseBalanceToExcel(dto, modelPath, balancePath);

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(result.getExcelPath()))) {
            Sheet balN = wb.getSheet("BAL N");
            assertThat(balN.getRow(1).getCell(0).getStringCellValue()).isEqualTo("601000");
            assertThat(balN.getRow(1).getCell(2).getNumericCellValue()).isEqualTo(150000.0);
            assertThat(balN.getRow(2).getCell(0).getStringCellValue()).isEqualTo("701000");
        }
    }

    @Test
    void fuseBalanceToExcel_rowsBeyond199AreIgnored() throws Exception {
        // construit une balance de 205 lignes de compte, vérifie que balN.getRow(200)
        // (= 200e ligne de données) reste vide après fusion — documente la limite actuelle.
    }

    @Test
    void fuseBalanceToExcel_whenModelHasNoBalNSheet_throwsNullPointerException() throws Exception {
        // modèle sans feuille "BAL N" (modelWithSheet("Autre Feuille", 10)) : documente que
        // le comportement actuel est bien une NullPointerException (pas une erreur métier
        // explicite) — ne pas la remplacer par une exception plus parlante ici, ce serait un
        // changement de comportement hors périmètre de ce plan.
        String modelPath = WorkbookTestFixtures.modelWithSheet("Autre Feuille", 10);
        String balancePath = WorkbookTestFixtures.balanceWorkbook(List.of(new Object[]{"601000", "Achats", 100.0}));
        EtatFinancierDto dto = EtatFinancierDto.builder().id(1L)
            .entreprise(EntrepriseSubstituteDto.builder().nameEntreprise("Acme").build())
            .annee_n(2025).build();

        assertThatThrownBy(() -> excelService.fuseBalanceToExcel(dto, modelPath, balancePath))
            .isInstanceOf(NullPointerException.class);
    }

    @Test
    void fuseBalanceToExcel_copiesBlankSourceCellsAsBlankDestinationCells() throws Exception {
        // la balance source contient une cellule BLANK en colonne D (ligne de compte "601000") ;
        // documente que la cellule correspondante dans "BAL N" devient BLANK après fusion
        // (branche `default: destCell.setBlank();` de addBalanceToModel).
        String modelPath = WorkbookTestFixtures.modelWithSheet("BAL N", 10);
        String balancePath = WorkbookTestFixtures.balanceWorkbookWithBlankCell("601000", /* col */ 3);
        EtatFinancierDto dto = EtatFinancierDto.builder().id(1L)
            .entreprise(EntrepriseSubstituteDto.builder().nameEntreprise("Acme").build())
            .annee_n(2025).build();

        EtatFinancierDto result = excelService.fuseBalanceToExcel(dto, modelPath, balancePath);

        try (Workbook wb = new XSSFWorkbook(new FileInputStream(result.getExcelPath()))) {
            assertThat(wb.getSheet("BAL N").getRow(1).getCell(3).getCellType()).isEqualTo(CellType.BLANK);
        }
    }
}
```

(Ajouter `balanceWorkbookWithBlankCell(String accountNumber, int blankColumnIndex)` à
`WorkbookTestFixtures` si absent de la Step 1.)

- [ ] **Step 3: Lancer les tests et vérifier qu'ils passent contre le code actuel**

Run: `mvn -Dtest=ExcelServiceCharacterizationTest test`
Expected: PASS (ce test documente le comportement **existant**, il ne doit pas échouer ici)

- [ ] **Step 4: Commit**

```bash
git add src/test/java/pluralisconseil/sn/pluralisEtatFin/helpers/
git commit -m "test: caractériser le comportement actuel de ExcelService.fuseBalanceToExcel"
```

---

## Task 2: Extraire `ExcelTemplateService`

**Files:**
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/helpers/ExcelTemplateService.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/helpers/impls/ExcelTemplateServiceImpl.java`
- Create: `src/test/java/pluralisconseil/sn/pluralisEtatFin/helpers/impls/ExcelTemplateServiceImplTest.java`
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/helpers/ExcelService.java:128-166` (supprimer `getExcelPageNames` et `setPartModelExcel` une fois déplacées)

**Interfaces:**
- Consumes: `WorkbookTestFixtures` (Task 1)
- Produces:
  ```java
  public interface ExcelTemplateService {
      List<String> getExcelPageNames(String path) throws Exception;
      void setPartModelExcel(String path, int part) throws Exception;
  }
  ```
  Utilisé par `EtatFinancierRestController` (Task 7) pour `GET /etats-fin/{id}/page-names`.

- [ ] **Step 1: Écrire les tests (avant extraction)**

```java
class ExcelTemplateServiceImplTest {
    @Test
    void getExcelPageNames_returnsAllSheetNamesInOrder() throws Exception {
        String path = WorkbookTestFixtures.modelWithSheets(List.of("BAL N", "BAL N-1", "Note 13"));
        assertThat(service.getExcelPageNames(path)).containsExactly("BAL N", "BAL N-1", "Note 13");
    }

    @Test
    void setPartModelExcel_writesPartIntoNote13Row7Col8() throws Exception {
        String path = WorkbookTestFixtures.modelWithSheet("Note 13", 10);
        service.setPartModelExcel(path, 3);
        try (Workbook wb = new XSSFWorkbook(new FileInputStream(path))) {
            assertThat(wb.getSheet("Note 13").getRow(7).getCell(8).getNumericCellValue()).isEqualTo(3.0);
        }
    }

    @Test
    void setPartModelExcel_noOpsWhenNote13Missing() throws Exception {
        String path = WorkbookTestFixtures.modelWithSheet("BAL N", 10);
        service.setPartModelExcel(path, 3); // ne doit pas lever d'exception
    }
}
```

(Ajouter `modelWithSheets(List<String>)` à `WorkbookTestFixtures` si absent de la Task 1.)

- [ ] **Step 2: Lancer les tests, vérifier qu'ils échouent** (classe inexistante)

Run: `mvn -Dtest=ExcelTemplateServiceImplTest test`
Expected: FAIL — `ExcelTemplateServiceImpl` n'existe pas

- [ ] **Step 3: Créer l'interface et déplacer `getExcelPageNames`/`setPartModelExcel` de `ExcelService` vers `ExcelTemplateServiceImpl`**

Copie identique du corps des deux méthodes (`ExcelService.java:128-145` et `:147-166`),
dépendance à `HelperService` conservée. Supprimer les deux méthodes de `ExcelService`.

- [ ] **Step 4: Lancer les tests, vérifier qu'ils passent**

Run: `mvn -Dtest=ExcelTemplateServiceImplTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/.../helpers/ExcelTemplateService.java src/main/java/.../helpers/impls/ExcelTemplateServiceImpl.java src/test/java/.../ExcelTemplateServiceImplTest.java src/main/java/.../helpers/ExcelService.java
git commit -m "refactor: extraire ExcelTemplateService de ExcelService"
```

---

## Task 3: Extraire `ExcelCleanupService`

**Files:**
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/helpers/ExcelCleanupService.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/helpers/impls/ExcelCleanupServiceImpl.java`
- Create: `src/test/java/pluralisconseil/sn/pluralisEtatFin/helpers/impls/ExcelCleanupServiceImplTest.java`
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/helpers/ExcelService.java:503-543` (supprimer `nettoyerEtRecupererExcel`)

**Interfaces:**
- Consumes: `WorkbookTestFixtures.modelWithSheets` (Task 2)
- Produces:
  ```java
  public interface ExcelCleanupService {
      void nettoyerEtRecupererExcel(String path) throws IOException;
  }
  ```
  Utilisé par `EtatFinancierImportServiceImpl` (Task 6) et `EtatFinancierRestController`
  (Task 7, endpoint `downloadFile`).

- [ ] **Step 1: Écrire le test**

```java
class ExcelCleanupServiceImplTest {
    @Test
    void nettoyerEtRecupererExcel_removesEvaluationWarningSheetsOnly() throws Exception {
        String path = WorkbookTestFixtures.modelWithSheets(
            List.of("BAL N", "Evaluation Warning", "Note 13"));
        service.nettoyerEtRecupererExcel(path);
        try (Workbook wb = new XSSFWorkbook(new FileInputStream(path))) {
            assertThat(wb.getNumberOfSheets()).isEqualTo(2);
            assertThat(wb.getSheet("Evaluation Warning")).isNull();
            assertThat(wb.getSheet("BAL N")).isNotNull();
        }
    }
}
```

- [ ] **Step 2: Lancer le test, vérifier qu'il échoue**

Run: `mvn -Dtest=ExcelCleanupServiceImplTest test`
Expected: FAIL — classe inexistante

- [ ] **Step 3: Créer l'interface et déplacer `nettoyerEtRecupererExcel` (`ExcelService.java:503-543`) vers `ExcelCleanupServiceImpl`**

Copie identique, suppression de l'original.

- [ ] **Step 4: Lancer le test, vérifier qu'il passe**

Run: `mvn -Dtest=ExcelCleanupServiceImplTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/.../helpers/ExcelCleanupService.java src/main/java/.../helpers/impls/ExcelCleanupServiceImpl.java src/test/java/.../ExcelCleanupServiceImplTest.java src/main/java/.../helpers/ExcelService.java
git commit -m "refactor: extraire ExcelCleanupService de ExcelService"
```

---

## Task 4: Extraire `BalanceMergeService` (fusion de balance + configuration + complément)

Les méthodes restantes d'`ExcelService` après les Tasks 2 et 3 :
`addBalanceToModel` (privée), `fuseBalanceToExcel` (x2), `configExcelFile`, `complement` (x2),
`copyCellValue` (privée). Elles sont déplacées **telles quelles**, sans réécriture — c'est le
plan d'import simplifié (hors périmètre ici) qui remplacera leur contenu.

**Files:**
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/helpers/BalanceMergeService.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/helpers/impls/BalanceMergeServiceImpl.java`
- Move: `src/test/java/pluralisconseil/sn/pluralisEtatFin/helpers/ExcelServiceCharacterizationTest.java` → `src/test/java/pluralisconseil/sn/pluralisEtatFin/helpers/impls/BalanceMergeServiceImplTest.java` (renommer la classe, remplacer `excelService` par `balanceMergeService`, sinon identique)
- Delete: `src/main/java/pluralisconseil/sn/pluralisEtatFin/helpers/ExcelService.java` (vide après ce déplacement)

**Interfaces:**
- Consumes: `WorkbookTestFixtures` (Task 1), `JsonService.getAllMatrice(boolean)` (existant, inchangé)
- Produces:
  ```java
  public interface BalanceMergeService {
      EtatFinancierDto fuseBalanceToExcel(EtatFinancierDto dto, String model_path, String balance_n_path) throws Exception;
      EtatFinancierDto fuseBalanceToExcel(EtatFinancierDto dto, String model_path, String balance_n_path, String balance_n_1_path) throws Exception;
      EtatFinancierDto configExcelFile(EtatFinancierDto dto, EntrepriseSubstituteDto entrepriseDto) throws Exception;
      void complement(boolean sage, String etatFinNpath, String etatFinN_1Path) throws Exception;
      void complement(boolean sage, String etatFinNpath, String etatFinN_1Path, String password) throws Exception;
  }
  ```
  Utilisé par `EtatFinancierImportServiceImpl` (Task 6).

- [ ] **Step 1: Déplacer et renommer le test de caractérisation de la Task 1**

```java
class BalanceMergeServiceImplTest {
    // identique à ExcelServiceCharacterizationTest, ciblant balanceMergeService
}
```

- [ ] **Step 2: Ajouter un test pour la surcharge à 2 balances**

```java
@Test
void fuseBalanceToExcel_withTwoBalances_fillsBalNAndBalN1() throws Exception {
    String modelPath = WorkbookTestFixtures.modelWithSheets(List.of("BAL N", "BAL N-1"));
    String balanceN = WorkbookTestFixtures.balanceWorkbook(List.of(new Object[]{"601000", "Achats", 100.0}));
    String balanceN1 = WorkbookTestFixtures.balanceWorkbook(List.of(new Object[]{"601000", "Achats", 90.0}));
    EtatFinancierDto dto = EtatFinancierDto.builder().id(1L)
        .entreprise(EntrepriseSubstituteDto.builder().nameEntreprise("Acme").build())
        .annee_n(2025).build();

    EtatFinancierDto result = balanceMergeService.fuseBalanceToExcel(dto, modelPath, balanceN, balanceN1);

    try (Workbook wb = new XSSFWorkbook(new FileInputStream(result.getExcelPath()))) {
        assertThat(wb.getSheet("BAL N").getRow(1).getCell(2).getNumericCellValue()).isEqualTo(100.0);
        assertThat(wb.getSheet("BAL N-1").getRow(1).getCell(2).getNumericCellValue()).isEqualTo(90.0);
    }
}
```

- [ ] **Step 3: Ajouter un smoke test documentant le bug de chemin de `complement(boolean, String, String)`**

```java
@Test
void complement_threeArgOverload_writesToHardcodedPathNotEtatFinNPath() throws Exception {
    // Documente le comportement actuel (voir Review Focus) : cette surcharge écrit
    // toujours dans "public/dossier/test2.xlsx", jamais dans etatFinNpath. Ne pas
    // "corriger" ce test pour le faire écrire au bon endroit — ce serait changer le
    // comportement, hors périmètre de ce plan.
    Files.deleteIfExists(Path.of("public/dossier/test2.xlsx"));
    balanceMergeService.complement(false, etatFinNPath, etatFinN1Path);
    assertThat(Files.exists(Path.of("public/dossier/test2.xlsx"))).isTrue();
    assertThat(new File(etatFinNPath).lastModified()).isEqualTo(etatFinNPathOriginalLastModified);
}
```

- [ ] **Step 4: Ajouter un smoke test nominal pour `configExcelFile`**

Un seul cas : un `ConfigModelExcelDto` pointant vers un champ existant de `EtatFinancierDto`
(ex. `annee_n`, type `NUMBER`), vérifier que la cellule ciblée contient la bonne valeur après
appel. Pas de couverture des 6 branches de `TypeAttributIsConfig` (hors périmètre, voir Review
Focus).

- [ ] **Step 5: Lancer tous les tests de `BalanceMergeServiceImplTest`, vérifier qu'ils échouent** (classe inexistante)

Run: `mvn -Dtest=BalanceMergeServiceImplTest test`
Expected: FAIL

- [ ] **Step 6: Créer l'interface `BalanceMergeService` et `BalanceMergeServiceImpl`, y déplacer `addBalanceToModel`, `fuseBalanceToExcel` (x2), `configExcelFile`, `complement` (x2), `copyCellValue` depuis `ExcelService.java` (lignes 33-111, 113-126, 168-501, 545-613, 615-639) — copie identique, aucune réécriture**

- [ ] **Step 7: Supprimer `ExcelService.java` (désormais vide)**

- [ ] **Step 8: Lancer tous les tests, vérifier qu'ils passent**

Run: `mvn -Dtest=BalanceMergeServiceImplTest test`
Expected: PASS

- [ ] **Step 9: Commit**

```bash
git add -A src/main/java/.../helpers/ src/test/java/.../helpers/
git commit -m "refactor: extraire BalanceMergeService, supprimer ExcelService"
```

---

## Task 5: Donner une interface à `AposePdfExcelService` et `JobConverterExcelPdflService`

Extraction d'interface pure, sans déplacer de code — ces deux classes restent des
collaborateurs internes à `EtatFinancierImportServiceImpl` (Task 6) et
`EtatFinancierRestController` (Task 7), jamais directement couplées à leur implémentation.

**Trouvé en lisant le code (corrige la lecture initiale du document de conception)** :
`AposePdfExcelService` déclare aussi deux méthodes `toPdf(String)`/`toPdf(String, List<String>)`
(`AposePdfExcelService.java:19,61`), identiques en signature à celles de
`JobConverterExcelPdflService` — mais **aucune n'est appelée nulle part** dans
`src/main/java` (seul `forceCalcul.calculateFormule(...)` est utilisé sur cette classe).
Seule `JobConverterExcelPdflService.toPdf(String, List<String>)` est réellement appelée
(`EtatFinancierRestController.java:324`) ; son overload à un seul argument
(`JobConverterExcelPdflService.java:22`) ne l'est pas non plus. `ExcelToPdfConverter` ne porte
donc qu'une seule méthode, et les trois overloads `toPdf` morts sont supprimés dans cette task
(pas de couverture de test à prévoir pour du code qui disparaît).

**Files:**
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/helpers/ExcelFormulaCalculator.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/helpers/ExcelToPdfConverter.java`
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/helpers/AposePdfExcelService.java`
  (implémente `ExcelFormulaCalculator` ; supprimer les deux méthodes `toPdf` mortes,
  lignes 19 et 61)
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/helpers/JobConverterExcelPdflService.java`
  (implémente `ExcelToPdfConverter` ; supprimer la méthode `toPdf(String)` morte, ligne 22)
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/EtatFinancierRestController.java:195`
  (supprimer la ligne commentée `// pdfService.calculateFormule(...)`, vestige d'un appel qui
  n'a jamais existé sur cette classe)

**Interfaces:**
- Produces:
  ```java
  public interface ExcelFormulaCalculator {
      void calculateFormule(String path) throws Exception;
  }
  public interface ExcelToPdfConverter {
      byte[] toPdf(String excelPath, List<String> pages);
  }
  ```
  Utilisés par `EtatFinancierImportServiceImpl` (Task 6, `ExcelFormulaCalculator`) et
  `EtatFinancierRestController` (Task 7, `ExcelToPdfConverter`).

- [ ] **Step 1: Déclarer `ExcelFormulaCalculator` et `ExcelToPdfConverter`** avec les signatures
  ci-dessus (copie exacte des signatures existantes de `calculateFormule` et de
  `toPdf(String, List<String>)`)

- [ ] **Step 2: Faire implémenter les interfaces par les deux classes existantes (ajout du
  mot-clé `implements`, sans toucher au corps des méthodes conservées), et supprimer les trois
  overloads `toPdf` morts identifiés ci-dessus ainsi que la ligne commentée du controller**

- [ ] **Step 3: Compiler le projet pour confirmer qu'aucun appelant n'est cassé**

Run: `mvn compile`
Expected: BUILD SUCCESS

- [ ] **Step 4: Commit**

```bash
git add src/main/java/.../helpers/ExcelFormulaCalculator.java src/main/java/.../helpers/ExcelToPdfConverter.java src/main/java/.../helpers/AposePdfExcelService.java src/main/java/.../helpers/JobConverterExcelPdflService.java src/main/java/.../api/controllers/EtatFinancierRestController.java
git commit -m "refactor: introduire ExcelFormulaCalculator/ExcelToPdfConverter, supprimer le code mort associé"
```

---

## Task 6: Créer `EtatFinancierImportService` (orchestrateur de l'import)

Déplace la logique métier aujourd'hui inline dans `EtatFinancierRestController.setBalances`,
`.setBalancesWithEtatFinN1`, `.setBalanceN` et le helper privé `.getObjectResponse`
(`EtatFinancierRestController.java:88-210`), y compris la suppression de l'enregistrement en
cas d'échec (actuellement faite par le controller dans chaque `catch`).

**Files:**
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/interfaces/EtatFinancierImportService.java`
- Create: `src/main/java/pluralisconseil/sn/pluralisEtatFin/services/impls/EtatFinancierImportServiceImpl.java`
- Create: `src/test/java/pluralisconseil/sn/pluralisEtatFin/services/impls/EtatFinancierImportServiceImplTest.java`

**Interfaces:**
- Consumes: `EtatFinancierService.get/update/delete` (existant), `ModelExcelService.get` (existant),
  `HelperService.saveTempFile/removesTemp` (existant), `BalanceMergeService` (Task 4),
  `ExcelCleanupService` (Task 3), `ExcelFormulaCalculator` (Task 5)
- Produces:
  ```java
  public interface EtatFinancierImportService {
      EtatFinancierDto importBalanceN(Long etatFinancierId, MultipartFile balanceN) throws Exception;
      EtatFinancierDto importBalances(Long etatFinancierId, MultipartFile balanceN, MultipartFile balanceN1) throws Exception;
      EtatFinancierDto importBalancesWithEtatFinancierN1(Long etatFinancierId, MultipartFile balanceN, MultipartFile balanceN1, MultipartFile etatFinancierN1) throws Exception;
  }
  ```
  Contrat : en cas d'échec (fichier manquant ou erreur de fusion/configuration), l'implémentation
  supprime l'enregistrement `EtatFinancier` (`etatFinancierId`) avant de relancer l'exception —
  réplique exacte du comportement actuel des blocs `catch` du controller. Utilisé par
  `EtatFinancierRestController` (Task 7).

- [ ] **Step 1: Écrire les tests avec Mockito (mocks pour toutes les dépendances)**

```java
@ExtendWith(MockitoExtension.class)
class EtatFinancierImportServiceImplTest {
    @Mock EtatFinancierService etatFinancierService;
    @Mock ModelExcelService modelExcelService;
    @Mock HelperService helperService;
    @Mock BalanceMergeService balanceMergeService;
    @Mock ExcelCleanupService excelCleanupService;
    @Mock ExcelFormulaCalculator excelFormulaCalculator;
    @InjectMocks EtatFinancierImportServiceImpl service;

    @Test
    void importBalances_mergesCleansCalculatesAndUpdates() throws Exception {
        // given : etatFinancierService.get(1L) retourne un dto avec modelExcel_id=5,
        // modelExcelService.get(5) retourne un ModelExcelDto avec excelPath="model.xlsx",
        // helperService.saveTempFile(any()) retourne des chemins temporaires fictifs,
        // balanceMergeService.fuseBalanceToExcel(...) retourne un dto avec excelPath="out.xlsx",
        // balanceMergeService.configExcelFile(...) retourne le même dto.
        // when : service.importBalances(1L, balanceN, balanceN1)
        // then : vérifier l'ordre d'appel (InOrder) fuseBalanceToExcel -> configExcelFile ->
        // excelCleanupService.nettoyerEtRecupererExcel -> excelFormulaCalculator.calculateFormule
        // -> etatFinancierService.update(...), et que helperService.removesTemp a bien été
        // appelé avec les deux chemins temporaires.
    }

    @Test
    void importBalances_onFailure_deletesEtatFinancierAndRethrows() throws Exception {
        // given : balanceMergeService.fuseBalanceToExcel(...) lève une RuntimeException
        // then : assertThatThrownBy(() -> service.importBalances(1L, balanceN, balanceN1))
        //   .isInstanceOf(RuntimeException.class);
        // verify(etatFinancierService).delete(1L);
    }
}
```

- [ ] **Step 2: Lancer les tests, vérifier qu'ils échouent** (classe inexistante)

Run: `mvn -Dtest=EtatFinancierImportServiceImplTest test`
Expected: FAIL

- [ ] **Step 3: Implémenter `EtatFinancierImportServiceImpl`**

Reprendre la logique exacte de `setBalances`/`setBalancesWithEtatFinN1`/`setBalanceN`/
`getObjectResponse` (`EtatFinancierRestController.java:88-210`), en remplaçant les appels à
`excelService`/`forceCalcul` par `balanceMergeService`/`excelFormulaCalculator`, et en
déplaçant les blocs `catch (FileNotFoundException f) { service.delete(id); ... }` /
`catch (Exception ex) { service.delete(id); ... }` du controller vers cette classe (qui
rethrow au lieu de construire la `Response` — c'est au controller de le faire, Task 7).

- [ ] **Step 4: Lancer les tests, vérifier qu'ils passent**

Run: `mvn -Dtest=EtatFinancierImportServiceImplTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/.../services/interfaces/EtatFinancierImportService.java src/main/java/.../services/impls/EtatFinancierImportServiceImpl.java src/test/java/.../EtatFinancierImportServiceImplTest.java
git commit -m "feat: ajouter EtatFinancierImportService, orchestrateur de l'import de balance"
```

---

## Task 7: Alléger `EtatFinancierRestController`

**Files:**
- Modify: `src/main/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/EtatFinancierRestController.java`
- Create: `src/test/java/pluralisconseil/sn/pluralisEtatFin/api/controllers/EtatFinancierRestControllerTest.java`

**Interfaces:**
- Consumes: `EtatFinancierImportService` (Task 6), `ExcelTemplateService` (Task 2),
  `ExcelCleanupService` (Task 3), `ExcelToPdfConverter` (Task 5)

- [ ] **Step 1: Écrire les tests `@WebMvcTest` pour les 3 endpoints d'import**

```java
@WebMvcTest(EtatFinancierRestController.class)
class EtatFinancierRestControllerTest {
    @MockBean EtatFinancierImportService importService;
    // ... autres mocks

    @Test
    void setBalances_delegatesToImportServiceAndReturns201() throws Exception {
        when(importService.importBalances(eq(1L), any(), any()))
            .thenReturn(EtatFinancierDto.builder().id(1L).build());

        mockMvc.perform(multipart("/etats-fin/1/annees-balances")
                .file("balance_n", "x".getBytes())
                .file("balance_n_1", "x".getBytes()))
            .andExpect(status().isCreated());
    }

    @Test
    void setBalances_whenFileNotFound_returns404() throws Exception {
        when(importService.importBalances(eq(1L), any(), any()))
            .thenThrow(new FileNotFoundException("Le fichier du model est introuvable"));

        mockMvc.perform(multipart("/etats-fin/1/annees-balances")
                .file("balance_n", "x".getBytes())
                .file("balance_n_1", "x".getBytes()))
            .andExpect(status().isNotFound());
    }
    // répéter pour setBalanceN, setBalancesWithEtatFinN1, et getPageNames (délègue à
    // ExcelTemplateService)
}
```

- [ ] **Step 2: Lancer les tests, vérifier qu'ils échouent** (le controller ne connaît pas
  encore `EtatFinancierImportService`)

Run: `mvn -Dtest=EtatFinancierRestControllerTest test`
Expected: FAIL

- [ ] **Step 3: Réécrire `EtatFinancierRestController`**

- Retirer les champs `mapper` (`EtatFinancierMapper`) et `entrepriseService`
  (`EntrepriseService`) — non utilisés dans le corps actuel du controller (vérifié : aucun
  appel à `mapper.` ni `entrepriseService.` dans les 373 lignes actuelles).
- Retirer les champs `excelService`, `forceCalcul`, `pdfService` ; ajouter `importService`
  (`EtatFinancierImportService`), `excelTemplateService` (`ExcelTemplateService`),
  `excelCleanupService` (`ExcelCleanupService`), `excelToPdfConverter` (`ExcelToPdfConverter`).
- `setBalances`/`setBalancesWithEtatFinN1`/`setBalanceN` : un seul appel à la méthode
  correspondante de `importService`, puis mapping de l'exception vers `Response` (le `try/catch`
  reste dans le controller — c'est la seule responsabilité HTTP qu'il garde).
- `downloadFile` : remplacer `excelService.nettoyerEtRecupererExcel` par
  `excelCleanupService.nettoyerEtRecupererExcel`.
- `getPageNames` : remplacer `excelService.getExcelPageNames` par
  `excelTemplateService.getExcelPageNames`.
- `exportExcelToPdfA4Marging` : remplacer `pdfService.toPdf` par `excelToPdfConverter.toPdf`.

- [ ] **Step 4: Lancer les tests, vérifier qu'ils passent**

Run: `mvn -Dtest=EtatFinancierRestControllerTest test`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src/main/java/.../api/controllers/EtatFinancierRestController.java src/test/java/.../EtatFinancierRestControllerTest.java
git commit -m "refactor: alléger EtatFinancierRestController, retirer mapper/entrepriseService inutilisés"
```

---

## Task 8: Régression complète et nettoyage final

**Files:**
- Verify only (pas de nouveau fichier)

- [ ] **Step 1: Lancer la suite complète**

Run: `mvn test`
Expected: BUILD SUCCESS, tous les tests des Tasks 1 à 7 passent

- [ ] **Step 2: Démarrer l'application et vérifier manuellement chaque endpoint**

Run: `mvn spring-boot:run` (profil `dev`), puis depuis le frontend Angular existant (ou `curl`/
Postman) : créer un état financier, importer une balance N seule, une balance N+N-1, télécharger
le fichier Excel, lister les pages, exporter un PDF — confirmer que chaque réponse est identique
à celle obtenue avant ce plan (même statut HTTP, mêmes données).

- [ ] **Step 3: Confirmer qu'`ExcelService.java` n'existe plus et qu'aucune référence n'y pointe**

Run: `grep -r "ExcelService" src/main/java --include="*.java"`
Expected: aucune occurrence (en dehors des noms des nouvelles classes
`ExcelTemplateService`/`ExcelCleanupService`/`BalanceMergeService`/`EtatFinancierImportService`)

- [ ] **Step 4: Commit final**

```bash
git add -A
git commit -m "chore: vérification finale du refactor architecture (god classes décomposées)"
```
