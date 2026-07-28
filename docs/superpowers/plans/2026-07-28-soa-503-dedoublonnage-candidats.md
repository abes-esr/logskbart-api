# SOA-503 — Dédoublonnage des candidats Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Conserver `CandidatsDoublons.xlsx` comme classeur cumulatif tout en empêchant l'ajout d'une ligne déjà présente et l'envoi d'un courriel sans nouvelle ligne.

**Architecture:** `CandidatsDoublonsService` indexe les huit cellules de chaque ligne existante sous forme de clés normalisées, puis filtre les candidats lus dans `_other.bad` avant toute écriture. Le contrat booléen de `append` reste inchangé : `true` signifie qu'au moins une nouvelle ligne a été écrite, ce qui pilote le courriel existant dans `LogsListener`.

**Tech Stack:** Java 21, Spring Boot, Apache POI, JUnit 5, Mockito, Maven.

## Global Constraints

- Branche : `feature/SOA-503-dedoublonnage-candidats`, basée sur `origin/develop`.
- Java 21 doit être conservé.
- Le classeur reste cumulatif et ses lignes historiques ne sont jamais supprimées.
- La clé d'unicité contient exactement les huit colonnes du classeur, normalisées uniquement avec `trim()`.
- La comparaison reste sensible à la casse et n'effectue aucun rapprochement flou.
- Aucun fichier temporaire ni remplacement atomique ne doit être effectué quand toutes les lignes sont déjà présentes.
- Le courriel « Candidats doublons » est envoyé uniquement lorsqu'au moins une nouvelle ligne est écrite.
- Les rapports `_400.bad`, `_other.bad` et leurs autres notifications ne doivent pas être modifiés.

---

### Task 1: Filtrer les candidats déjà présents avant l'écriture

**Files:**
- Modify: `src/test/java/fr/abes/logskbart/service/CandidatsDoublonsServiceTest.java`
- Modify: `src/main/java/fr/abes/logskbart/service/CandidatsDoublonsService.java:53-77`

**Interfaces:**
- Consumes: `List<CandidatDoublon> readCandidates(String filename)` et le classeur retourné par `openWorkbook()`.
- Produces: `boolean append(String filename)` retourne `true` uniquement si au moins une ligne nouvelle est écrite.
- Produces: `Set<List<String>> existingCandidateKeys(Sheet sheet)` contient les clés normalisées des lignes Excel existantes.
- Produces: `List<String> normalizedKey(List<String> values)` retourne huit chaînes passées par `trim()`.

- [ ] **Step 1: Ajouter les tests en échec pour un rejeu et pour un doublon interne**

Ajouter dans `CandidatsDoublonsServiceTest` :

```java
@Test
void doesNotAppendCandidateAlreadyPresentAndDoesNotRewriteWorkbook() throws IOException {
    String filename = "TEST_PROVIDER_PACKAGE_2026-07-16.tsv";
    String candidate = "1\tDoublon (111111111) [ publication title : Titre 1 "
            + "/ publication_type : serial / online_identifier : 1111-1111 "
            + "/ print_identifier : 2222-2222 ]";
    writeOtherReport(filename, candidate);
    CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);

    assertTrue(service.append(filename));
    FileTime marker = FileTime.from(Instant.parse("2020-01-01T00:00:00Z"));
    Files.setLastModifiedTime(service.workbookPath(), marker);

    assertFalse(service.append(filename));
    assertEquals(marker, Files.getLastModifiedTime(service.workbookPath()));
    assertFalse(Files.exists(service.workbookPath().resolveSibling("CandidatsDoublons.xlsx.tmp")));
    try (Workbook workbook = WorkbookFactory.create(service.workbookPath().toFile())) {
        assertEquals(1, workbook.getSheetAt(0).getLastRowNum());
    }
}

@Test
void appendsOnlyOnceWhenReportContainsTheSameCandidateTwice() throws IOException {
    String filename = "TEST_PROVIDER_PACKAGE_2026-07-16.tsv";
    String candidate = "1\tDoublon (111111111) [ publication title : Titre 1 "
            + "/ publication_type : serial / online_identifier : 1111-1111 "
            + "/ print_identifier : 2222-2222 ]";
    Files.writeString(
            badReportService.otherReportPath(filename),
            "LINE\tMESSAGE\t" + System.lineSeparator()
                    + candidate + System.lineSeparator()
                    + candidate + System.lineSeparator()
    );
    CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);

    assertTrue(service.append(filename));
    try (Workbook workbook = WorkbookFactory.create(service.workbookPath().toFile())) {
        assertEquals(1, workbook.getSheetAt(0).getLastRowNum());
    }
}
```

Ajouter les imports :

```java
import java.nio.file.attribute.FileTime;
import java.time.Instant;
```

- [ ] **Step 2: Exécuter les deux tests pour vérifier leur échec**

Run:

```bash
mvn -Dtest=CandidatsDoublonsServiceTest#doesNotAppendCandidateAlreadyPresentAndDoesNotRewriteWorkbook+appendsOnlyOnceWhenReportContainsTheSameCandidateTwice test
```

Expected: FAIL, car `append` retourne encore `true` au second passage et écrit deux lignes identiques.

- [ ] **Step 3: Ajouter le test en échec pour un mélange de lignes**

Ajouter :

```java
@Test
void appendsOnlyNewCandidatesWhenWorkbookContainsExistingRows() throws IOException {
    CandidatsDoublonsService service = new CandidatsDoublonsService(badReportService);
    writeOtherReport("SAME.tsv",
            "1\tDoublon (111111111) [ publication title : Existant "
                    + "/ publication_type : serial / online_identifier : 1111-1111 "
                    + "/ print_identifier : 2222-2222 ]");
    assertTrue(service.append("SAME.tsv"));

    Files.writeString(
            badReportService.otherReportPath("SAME.tsv"),
            "LINE\tMESSAGE\t" + System.lineSeparator()
                    + "1\tDoublon (111111111) [ publication title : Existant "
                    + "/ publication_type : serial / online_identifier : 1111-1111 "
                    + "/ print_identifier : 2222-2222 ]" + System.lineSeparator()
                    + "2\tDoublon (222222222) [ publication title : Nouveau "
                    + "/ publication_type : monograph / online_identifier : 3333-3333 "
                    + "/ print_identifier : 4444-4444 ]" + System.lineSeparator()
    );

    assertTrue(service.append("SAME.tsv"));
    try (Workbook workbook = WorkbookFactory.create(service.workbookPath().toFile())) {
        Sheet sheet = workbook.getSheetAt(0);
        assertEquals(2, sheet.getLastRowNum());
        assertEquals("111111111", sheet.getRow(1).getCell(0).getStringCellValue());
        assertEquals("222222222", sheet.getRow(2).getCell(0).getStringCellValue());
    }
}
```

Le même nom `SAME.tsv` est utilisé aux deux appels afin que la colonne « Nom du bouquet », incluse dans la clé, reste identique pour le candidat existant.

- [ ] **Step 4: Exécuter le test de mélange pour vérifier son échec**

Run:

```bash
mvn -Dtest=CandidatsDoublonsServiceTest#appendsOnlyNewCandidatesWhenWorkbookContainsExistingRows test
```

Expected: FAIL avec trois lignes de données au lieu de deux.

- [ ] **Step 5: Implémenter l'index et le filtrage minimal**

Ajouter les imports suivants à `CandidatsDoublonsService` :

```java
import org.apache.poi.ss.usermodel.DataFormatter;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.IntStream;
```

Réorganiser `append` pour filtrer avant de créer le fichier temporaire :

```java
public synchronized boolean append(String filename) throws IOException {
    List<CandidatDoublon> candidates = readCandidates(filename);
    if (candidates.isEmpty()) {
        return false;
    }

    Path temporaryPath;
    int newCandidateCount;
    try (Workbook workbook = openWorkbook()) {
        Sheet sheet = workbook.getSheet(SHEET_NAME);
        if (sheet == null) {
            sheet = createSheet(workbook);
        }

        Set<List<String>> knownKeys = existingCandidateKeys(sheet);
        List<CandidatDoublon> newCandidates = candidates.stream()
                .filter(candidate -> knownKeys.add(normalizedKey(candidate.values())))
                .toList();
        if (newCandidates.isEmpty()) {
            return false;
        }
        newCandidateCount = newCandidates.size();

        CellStyle textStyle = textStyle(workbook);
        int rowIndex = sheet.getLastRowNum() + 1;
        for (CandidatDoublon candidate : newCandidates) {
            writeRow(sheet.createRow(rowIndex++), candidate.values(), textStyle);
        }
        updateAutoFilter(sheet);
        temporaryPath = temporaryWorkbookPath();
        writeTemporary(workbook, temporaryPath);
    }
    replaceWorkbook(temporaryPath);

    log.info("{} nouveau(x) candidat(s) ajouté(s) dans {} pour le fichier {}",
            newCandidateCount, workbookPath(), filename);
    return true;
}
```

Ajouter les méthodes :

```java
private Set<List<String>> existingCandidateKeys(Sheet sheet) {
    DataFormatter formatter = new DataFormatter();
    Set<List<String>> keys = new HashSet<>();
    IntStream.rangeClosed(1, sheet.getLastRowNum())
            .mapToObj(sheet::getRow)
            .filter(row -> row != null)
            .map(row -> IntStream.range(0, HEADERS.size())
                    .mapToObj(index -> formatter.formatCellValue(row.getCell(index)))
                    .toList())
            .map(this::normalizedKey)
            .forEach(keys::add);
    return keys;
}

private List<String> normalizedKey(List<String> values) {
    return values.stream()
            .map(value -> value == null ? "" : value.trim())
            .toList();
}
```

- [ ] **Step 6: Exécuter tous les tests du service**

Run:

```bash
mvn -Dtest=CandidatsDoublonsServiceTest test
```

Expected: tous les tests de `CandidatsDoublonsServiceTest` passent, notamment les trois nouveaux scénarios.

- [ ] **Step 7: Vérifier explicitement le contrat de notification**

Run:

```bash
mvn -Dtest=LogsListenerTest#doesNotSendCandidatesEmailWhenNoCandidateWasExported+LogsListenerTest#sendsImmediateEmailsForOtherErrorsAndCandidates test
```

Expected: les deux tests passent ; `append == false` n'envoie aucun courriel de candidats et `append == true` en envoie un.

- [ ] **Step 8: Exécuter la régression complète**

Run:

```bash
mvn test
```

Expected: au moins 39 tests, 0 échec, 0 erreur, sous Java 21.

- [ ] **Step 9: Vérifier le diff et committer**

Run:

```bash
git diff --check
git status --short
git diff -- src/main/java/fr/abes/logskbart/service/CandidatsDoublonsService.java src/test/java/fr/abes/logskbart/service/CandidatsDoublonsServiceTest.java
git add src/main/java/fr/abes/logskbart/service/CandidatsDoublonsService.java src/test/java/fr/abes/logskbart/service/CandidatsDoublonsServiceTest.java
git -c user.name="Jerome Villiseck" -c user.email="jvk@abes.fr" commit -m "Éviter les doublons dans le rapport des candidats"
```

Expected: un commit fonctionnel indépendant, sans fichier généré ni modification hors périmètre.
