# Conversion XLSX de CandidatsDoublons — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remplacer les futurs ajouts TSV de `CandidatsDoublons` par un classeur XLSX cumulatif, lisible et sûr, sans migrer le fichier TXT historique.

**Architecture:** Un service `CandidatsDoublonsService` lit le `_other.bad`, extrait les candidats structurés et écrit le classeur avec Apache POI. Il sérialise les écritures dans la JVM et remplace le fichier final à partir d'un temporaire. `LogsListener` ne fait plus de parsing XLSX et l'email pointe vers le nouveau classeur.

**Tech Stack:** Java 21, Spring Boot, Apache POI `poi-ooxml` 5.2.5, JUnit 5, Mockito, Maven.

## Global Constraints

- Projet unique : `logskbart-api`.
- Branche : `feature/SOA-503-candidats-doublons-xlsx`.
- Java 21.
- Apache POI 5.2.5.
- Fichier final : `${abes.path-to-reports}/CandidatsDoublons.xlsx`.
- Feuille : `CandidatsDoublons`.
- Colonnes exactes : `PPN`, `Commande WinIBW`, `Titre`, `Type de ressource`, `ISSN imprimé`, `ISSN en ligne`, `Nom du bouquet`, `Nature de l'erreur`.
- Toutes les cellules de données sont de type texte.
- `CandidatsDoublons.txt` est conservé sans lecture, modification ou suppression.
- Aucun dédoublonnage des lignes.

---

### Task 1: Dépendance Apache POI et tests du format

**Files:**
- Modify: `pom.xml`
- Create: `src/test/java/fr/abes/logskbart/service/CandidatsDoublonsServiceTest.java`

**Interfaces:**
- Produces: dépendance `org.apache.poi:poi-ooxml:5.2.5` disponible en production et dans les tests.
- Defines expected API: `boolean CandidatsDoublonsService.append(String filename) throws IOException`.

- [ ] **Step 1: Add Apache POI dependency**

Ajouter dans `<dependencies>` :

```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```

- [ ] **Step 2: Verify dependency resolution**

Run: `mvn -DskipTests compile`

Expected: `BUILD SUCCESS`.

- [ ] **Step 3: Write failing creation test**

Dans un `@TempDir`, créer `bad/TEST_PACKAGE_other.bad` avec un message structuré complet. Instancier le service souhaité avec `new CandidatsDoublonsService(new BadReportService(tempDir.toString()))`, appeler `append("TEST_PACKAGE.tsv")`, puis ouvrir `CandidatsDoublons.xlsx` avec `WorkbookFactory`.

Vérifier :

```java
assertTrue(appended);
assertEquals("CandidatsDoublons", workbook.getSheetAt(0).getSheetName());
assertEquals(List.of(
        "PPN", "Commande WinIBW", "Titre", "Type de ressource",
        "ISSN imprimé", "ISSN en ligne", "Nom du bouquet", "Nature de l'erreur"
), headerValues(sheet));
assertEquals("290245540 OU 289331811", sheet.getRow(1).getCell(0).getStringCellValue());
assertEquals("che ppn 290245540 OU 289331811", sheet.getRow(1).getCell(1).getStringCellValue());
```

Contrôler également titre, type, ISSN imprimé, ISSN en ligne, bouquet et nature d'erreur dans les colonnes C à H.

- [ ] **Step 4: Run creation test and verify RED**

Run: `mvn -Dtest=CandidatsDoublonsServiceTest test`

Expected: compilation failure because `CandidatsDoublonsService` does not exist.

---

### Task 2: Création et append atomique du classeur

**Files:**
- Create: `src/main/java/fr/abes/logskbart/service/CandidatsDoublonsService.java`
- Modify: `src/test/java/fr/abes/logskbart/service/CandidatsDoublonsServiceTest.java`

**Interfaces:**
- Consumes: `BadReportService.otherReportPath(String)` and `BadReportService.reportDirectory()`.
- Produces: `public synchronized boolean append(String filename) throws IOException`.
- Produces: `Path workbookPath()` for locating the application artifact.

- [ ] **Step 1: Implement minimal XLSX creation**

Créer un `@Service` injectant `BadReportService`. `append` lit le `_other.bad` en UTF-8, saute l'en-tête, extrait uniquement les messages contenant `publication title : `, et retourne `false` sans créer de XLSX lorsqu'aucun candidat n'est extrait.

Pour chaque candidat, construire les huit valeurs dans l'ordre défini. Le bouquet est obtenu avec :

```java
filename.replaceFirst("(?i)\\.tsv$", "").replaceAll("_(FORCE|BYPASS)$", "")
```

Créer un `XSSFWorkbook`, la feuille `CandidatsDoublons`, les en-têtes et les lignes. Écrire d'abord dans `CandidatsDoublons.xlsx.tmp`, fermer le workbook et le flux, puis déplacer le temporaire vers le XLSX avec `ATOMIC_MOVE` et `REPLACE_EXISTING`, avec repli sans `ATOMIC_MOVE`.

- [ ] **Step 2: Run creation test and verify GREEN**

Run: `mvn -Dtest=CandidatsDoublonsServiceTest test`

Expected: creation test passes.

- [ ] **Step 3: Write failing append and preservation tests**

Ajouter des tests qui :

- appellent `append` pour deux bouquets et vérifient une ligne d'en-tête plus deux lignes de données ;
- vérifient que la première ligne reste inchangée ;
- créent `CandidatsDoublons.txt` avant l'appel et vérifient que son contenu reste identique ;
- utilisent `_FORCE.tsv` et `_BYPASS.tsv` et vérifient que ces suffixes ne figurent pas dans la colonne G ;
- fournissent uniquement des lignes non structurées et vérifient `false` ainsi que l'absence du XLSX.

- [ ] **Step 4: Run expanded tests and verify RED where behavior is missing**

Run: `mvn -Dtest=CandidatsDoublonsServiceTest test`

Expected: append/format tests fail until existing workbook loading and format are implemented.

- [ ] **Step 5: Implement append and workbook format**

Si le fichier existe, l'ouvrir avec `WorkbookFactory.create(InputStream)` ; sinon créer `XSSFWorkbook`. Ajouter les lignes après `sheet.getLastRowNum()`.

Lors de la création :

- style d'en-tête en gras, blanc, fond bleu foncé ;
- `sheet.createFreezePane(0, 1)` ;
- filtre `new CellRangeAddress(0, sheet.getLastRowNum(), 0, 7)` ;
- largeurs fixes bornées pour les huit colonnes.

Utiliser `Cell#setCellValue(String)` pour toutes les cellules, avec chaîne vide pour un champ absent. Mettre à jour le filtre après chaque append.

- [ ] **Step 6: Run service tests and verify GREEN**

Run: `mvn -Dtest=CandidatsDoublonsServiceTest test`

Expected: all service tests pass.

---

### Task 3: Intégration listener et email XLSX

**Files:**
- Modify: `src/main/java/fr/abes/logskbart/kafka/LogsListener.java`
- Modify: `src/main/java/fr/abes/logskbart/service/EmailService.java`
- Modify: `src/test/java/fr/abes/logskbart/kafka/LogsListenerTest.java`
- Modify: `src/test/java/fr/abes/logskbart/service/EmailServiceTest.java`

**Interfaces:**
- Consumes: `CandidatsDoublonsService.append(String)`.
- Changes: constructeur de `LogsListener` reçoit `CandidatsDoublonsService`.
- Changes: `EmailService.sendCandidatsDoublonsEmail(String)` links to `CandidatsDoublons.xlsx`.

- [ ] **Step 1: Write failing listener tests**

Mocker `CandidatsDoublonsService`. Pour `BadReportResult(false, true)`, vérifier l'email `_other.bad`, l'appel `append(filename)`, puis l'email CandidatsDoublons lorsque `append` retourne `true`.

Ajouter un cas `append` retournant `false` :

```java
verify(emailService).sendOtherErrorsEmail(filename);
verify(emailService, never()).sendCandidatsDoublonsEmail(filename);
```

- [ ] **Step 2: Write failing email-link test**

Appeler `sendCandidatsDoublonsEmail` sur la sous-classe de capture existante et vérifier que le JSON contient `CandidatsDoublons.xlsx` et ne contient pas `CandidatsDoublons.txt`.

- [ ] **Step 3: Run integration tests and verify RED**

Run: `mvn -Dtest=LogsListenerTest,EmailServiceTest test`

Expected: tests fail because the listener still writes the TXT and the email still links `.txt`.

- [ ] **Step 4: Integrate the service**

Injecter `CandidatsDoublonsService` dans `LogsListener`. Dans `notifyReports`, conserver l'email hors 400, puis :

```java
if (candidatsDoublonsService.append(filename)) {
    emailService.sendCandidatsDoublonsEmail(filename);
}
```

Supprimer de `LogsListener` l'ancienne génération TSV et ses imports devenus inutiles. Adapter ses tests de parsing/append en tests du nouveau service.

- [ ] **Step 5: Update email link**

Modifier le sujet, le lien et le libellé de `sendCandidatsDoublonsEmail` pour utiliser `CandidatsDoublons.xlsx`.

- [ ] **Step 6: Run integration tests and verify GREEN**

Run: `mvn -Dtest=LogsListenerTest,EmailServiceTest,CandidatsDoublonsServiceTest test`

Expected: all targeted tests pass.

---

### Task 4: Vérification complète et visuelle

**Files:**
- Verify: all modified production and test files.
- Temporary verification artifacts only under `target/xlsx-preview/`.

- [ ] **Step 1: Run full test suite**

Run: `mvn clean test`

Expected: `BUILD SUCCESS`, zero failures, zero errors.

- [ ] **Step 2: Generate representative workbook with production service**

Compiler le projet et construire le classpath Maven. Utiliser un script JShell temporaire sous `target/xlsx-preview/` pour créer un `_other.bad` représentatif et appeler `CandidatsDoublonsService.append`. Le classeur produit doit rester uniquement sous `target/`.

- [ ] **Step 3: Inspect and render workbook**

Importer le classeur produit avec le runtime tableur fourni, inspecter `CandidatsDoublons!A1:H2`, vérifier l'absence d'erreur de formule, puis rendre la feuille en PNG. Contrôler visuellement que les huit en-têtes et la ligne de données ne sont pas tronqués.

- [ ] **Step 4: Run diff checks**

Run: `git diff --check`

Expected: exit code 0.

- [ ] **Step 5: Review requirement coverage**

Vérifier dans le diff : POI 5.2.5, huit colonnes dans l'ordre, données texte, append, écriture temporaire, synchronisation, TXT intact, lien email XLSX et absence de modification de la PR #65.

- [ ] **Step 6: Leave implementation uncommitted for review**

Run: `git status --short`

Expected: only source, tests and `pom.xml` are modified or untracked ; `target/` remains ignored.
