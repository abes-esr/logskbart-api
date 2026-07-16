# Récapitulatif quotidien des erreurs 400 — Plan d'implémentation

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Envoyer à 23 h un unique email contenant les liens vers les `_400.bad` non encore récapitulés, tout en conservant l'email immédiat pour `_other.bad`.

**Architecture:** Le système de fichiers durable reste la source de vérité. Un sélecteur trouve les rapports modifiés dans une fenêtre temporelle, un magasin d'état persiste la dernière borne traitée, et `DailyRecapScheduler` n'avance cette borne qu'après un envoi réussi. `EmailService` expose séparément les notifications quotidiennes 400 et immédiates hors 400.

**Tech Stack:** Java 21, Spring Boot, Spring Scheduling, JUnit 5, Mockito, Maven.

## Global Constraints

- Projet unique : `logskbart-api`.
- Java 21.
- Cron par défaut : `0 0 23 * * *`.
- Fuseau par défaut : `Europe/Paris`.
- Aucun email immédiat pour un traitement contenant uniquement des erreurs 400.
- Les `_other.bad` restent notifiés immédiatement.
- L'état doit survivre aux redémarrages et ne doit avancer qu'après un envoi réussi.
- Aucun fichier `.bad` n'est supprimé par cette fonctionnalité.

---

### Task 1: Sélection durable des rapports 400

**Files:**
- Create: `src/main/java/fr/abes/logskbart/service/Daily400ReportFinder.java`
- Create: `src/main/java/fr/abes/logskbart/service/DailyRecapStateStore.java`
- Test: `src/test/java/fr/abes/logskbart/service/Daily400ReportFinderTest.java`
- Test: `src/test/java/fr/abes/logskbart/service/DailyRecapStateStoreTest.java`

**Interfaces:**
- Produces: `List<Path> Daily400ReportFinder.find(Instant afterExclusive, Instant upToInclusive)`.
- Produces: `Optional<Instant> DailyRecapStateStore.read()`.
- Produces: `void DailyRecapStateStore.write(Instant instant)`.

- [ ] **Step 1: Write failing finder tests**

Créer trois fichiers sous `@TempDir/bad`, régler leurs dates avec `Files.setLastModifiedTime`, puis vérifier que seuls les fichiers réguliers suffixés `_400.bad` et compris dans `]afterExclusive, upToInclusive]` sont retournés, triés par nom.

```java
List<Path> reports = finder.find(start, cutoff);
assertEquals(List.of(reportA, reportB), reports);
```

- [ ] **Step 2: Run finder tests and verify RED**

Run: `mvn -Dtest=Daily400ReportFinderTest test`

Expected: compilation failure because `Daily400ReportFinder` does not exist.

- [ ] **Step 3: Implement the finder**

`Daily400ReportFinder` reçoit `abes.path-to-reports`, résout `bad/`, retourne une liste vide si le dossier n'existe pas, utilise `Files.list`, `Files.isRegularFile`, le suffixe `_400.bad`, `Files.getLastModifiedTime`, puis trie par nom de fichier.

- [ ] **Step 4: Run finder tests and verify GREEN**

Run: `mvn -Dtest=Daily400ReportFinderTest test`

Expected: all finder tests pass.

- [ ] **Step 5: Write failing state-store tests**

Vérifier qu'un état absent produit `Optional.empty()`, qu'un instant écrit est relu après création d'une nouvelle instance, et qu'un contenu invalide lève `IOException`.

```java
store.write(Instant.parse("2026-07-16T21:00:00Z"));
assertEquals(expected, new DailyRecapStateStore(tempDir.toString()).read().orElseThrow());
```

- [ ] **Step 6: Run state-store tests and verify RED**

Run: `mvn -Dtest=DailyRecapStateStoreTest test`

Expected: compilation failure because `DailyRecapStateStore` does not exist.

- [ ] **Step 7: Implement the state store**

Utiliser `${abes.path-to-reports}/.daily-400-recap-state`. Écrire l'ISO-8601 dans un fichier temporaire voisin puis le remplacer avec `StandardCopyOption.REPLACE_EXISTING`; tenter `ATOMIC_MOVE` et retomber sur un déplacement normal si le système ne le supporte pas.

- [ ] **Step 8: Run both test classes and verify GREEN**

Run: `mvn -Dtest=Daily400ReportFinderTest,DailyRecapStateStoreTest test`

Expected: all selection and persistence tests pass.

---

### Task 2: Emails quotidien et immédiat

**Files:**
- Modify: `src/main/java/fr/abes/logskbart/service/EmailService.java`
- Create: `src/test/java/fr/abes/logskbart/service/EmailServiceTest.java`

**Interfaces:**
- Produces: `void EmailService.sendOtherErrorsEmail(String packageName)`.
- Produces: `boolean EmailService.sendDailyRecapEmail(List<String> filenames)`.
- Changes: `protected boolean EmailService.sendMail(String requestJson)`.

- [ ] **Step 1: Write failing email tests**

Créer une sous-classe de test qui capture le JSON et choisit le résultat de `sendMail`. Injecter `recipient`, `env` et `serveurUrl` avec `ReflectionTestUtils`.

Vérifier :

```java
service.sendOtherErrorsEmail("TEST_PACKAGE.tsv");
assertTrue(json.contains("bad/TEST_PACKAGE_other.bad"));

boolean sent = service.sendDailyRecapEmail(List.of("A_400.bad", "B_400.bad"));
assertTrue(sent);
assertTrue(json.contains("bad/A_400.bad"));
assertTrue(json.contains("bad/B_400.bad"));
assertEquals(1, sendCount);
```

- [ ] **Step 2: Run email tests and verify RED**

Run: `mvn -Dtest=EmailServiceTest test`

Expected: compilation failure because the two public methods do not exist and `sendMail` returns `void`.

- [ ] **Step 3: Implement email behavior**

Remplacer l'ancien `sendEmail` par `sendOtherErrorsEmail`, avec un lien `bad/<base>_other.bad`. Ajouter `sendDailyRecapEmail`, qui trie les noms, construit un lien HTML par fichier et appelle `sendMail` exactement une fois. Faire retourner `true` à `sendMail` après un POST réussi et `false` dans le `catch`.

- [ ] **Step 4: Run email tests and verify GREEN**

Run: `mvn -Dtest=EmailServiceTest test`

Expected: all email tests pass.

---

### Task 3: Orchestration quotidienne

**Files:**
- Create: `src/main/java/fr/abes/logskbart/service/DailyRecapScheduler.java`
- Create: `src/test/java/fr/abes/logskbart/service/DailyRecapSchedulerTest.java`

**Interfaces:**
- Consumes: `Daily400ReportFinder.find(Instant, Instant)`.
- Consumes: `DailyRecapStateStore.read()` and `write(Instant)`.
- Consumes: `EmailService.sendDailyRecapEmail(List<String>)`.
- Produces: `void DailyRecapScheduler.runDailyRecap() throws IOException` for deterministic unit testing.
- Produces: scheduled wrapper `void DailyRecapScheduler.scheduleDailyRecap()`.

- [ ] **Step 1: Write failing scheduler tests**

Avec Mockito et un `Clock.fixed` à `2026-07-16T21:00:00Z` en `Europe/Paris`, couvrir :

1. état absent : le finder reçoit le début de journée et la coupure ;
2. deux rapports : un seul email reçoit les deux noms et l'état est avancé après succès ;
3. aucun rapport : aucun email et l'état est avancé ;
4. email en échec : l'état n'est pas avancé ;
5. état existant : cette valeur est utilisée comme borne basse.

```java
when(emailService.sendDailyRecapEmail(List.of("A_400.bad", "B_400.bad"))).thenReturn(true);
scheduler.runDailyRecap();
verify(stateStore).write(cutoff);
```

- [ ] **Step 2: Run scheduler tests and verify RED**

Run: `mvn -Dtest=DailyRecapSchedulerTest test`

Expected: compilation failure because `DailyRecapScheduler` does not exist.

- [ ] **Step 3: Implement scheduler**

Injecter finder, state store, email service et `Clock`. `runDailyRecap` calcule la borne initiale au début de la journée du `Clock` si l'état est absent, extrait uniquement les noms de fichiers, n'avance l'état qu'en cas de succès, et l'avance sans email lorsqu'il n'y a aucun rapport. Le wrapper annoté :

```java
@Scheduled(cron = "${abes.daily-recap.cron}", zone = "${abes.daily-recap.zone}")
public void scheduleDailyRecap() {
    try {
        runDailyRecap();
    } catch (IOException exception) {
        log.error("Impossible de générer le récapitulatif quotidien des erreurs 400", exception);
    }
}
```

- [ ] **Step 4: Run scheduler tests and verify GREEN**

Run: `mvn -Dtest=DailyRecapSchedulerTest test`

Expected: all scheduler tests pass.

---

### Task 4: Intégration Spring et comportement immédiat

**Files:**
- Create: `src/main/java/fr/abes/logskbart/configuration/DailyRecapConfiguration.java`
- Modify: `src/main/java/fr/abes/logskbart/kafka/LogsListener.java`
- Modify: `src/main/resources/application.properties`
- Modify: `src/test/java/fr/abes/logskbart/kafka/LogsListenerTest.java`
- Test: `src/test/java/fr/abes/logskbart/configuration/DailyRecapConfigurationTest.java`

**Interfaces:**
- Produces: bean `Clock dailyRecapClock` dans le fuseau configuré.
- Changes: `LogsListener.notifyReports(String, BadReportResult)` déclenche les notifications immédiates uniquement si `hasOtherErrors()` vaut `true`.

- [ ] **Step 1: Write failing listener notification tests**

Construire `LogsListener` avec un mock `EmailService`. Vérifier qu'un `BadReportResult(true, false)` n'appelle aucune méthode d'email. Pour `BadReportResult(false, true)`, créer le `_other.bad` attendu et vérifier exactement un appel à `sendOtherErrorsEmail(packageName)` et un à `sendCandidatsDoublonsEmail(packageName)`.

- [ ] **Step 2: Run listener tests and verify RED**

Run: `mvn -Dtest=LogsListenerTest test`

Expected: compilation failure because `notifyReports` and `sendOtherErrorsEmail` are not yet integrated.

- [ ] **Step 3: Implement listener notification boundary**

Après `createFileBad`, appeler `notifyReports`. Cette méthode retourne immédiatement sans autre erreur ; sinon elle envoie le lien `_other.bad`, alimente `CandidatsDoublons`, puis envoie son email. Aucun appel immédiat n'est effectué sur la seule présence de `has400Errors`.

- [ ] **Step 4: Run listener tests and verify GREEN**

Run: `mvn -Dtest=LogsListenerTest test`

Expected: all listener tests pass.

- [ ] **Step 5: Write failing configuration test**

Créer un contexte Spring minimal avec `abes.daily-recap.zone=Europe/Paris` et vérifier que le bean `Clock` utilise `ZoneId.of("Europe/Paris")`.

- [ ] **Step 6: Run configuration test and verify RED**

Run: `mvn -Dtest=DailyRecapConfigurationTest test`

Expected: compilation failure because `DailyRecapConfiguration` does not exist.

- [ ] **Step 7: Implement scheduling configuration**

Créer une configuration `@Configuration` et `@EnableScheduling` exposant :

```java
@Bean
Clock dailyRecapClock(@Value("${abes.daily-recap.zone}") String zone) {
    return Clock.system(ZoneId.of(zone));
}
```

Ajouter à `application.properties` :

```properties
abes.daily-recap.cron=0 0 23 * * *
abes.daily-recap.zone=Europe/Paris
```

- [ ] **Step 8: Run configuration and integration tests**

Run: `mvn -Dtest=DailyRecapConfigurationTest,LogsListenerTest test`

Expected: all tests pass.

---

### Task 5: Vérification globale

**Files:**
- Verify all modified and created files.

- [ ] **Step 1: Run formatting/diff checks**

Run: `git diff --check`

Expected: exit code 0.

- [ ] **Step 2: Run the full Maven suite**

Run: `mvn clean test`

Expected: `BUILD SUCCESS`, zero failures and zero errors.

- [ ] **Step 3: Review requirement coverage**

Vérifier dans le diff : cron 23 h, fuseau Paris, scan durable, reprise après échec, email unique quotidien, email `_other.bad` immédiat et aucun email 400 immédiat.

- [ ] **Step 4: Leave implementation uncommitted for user review**

Run: `git status --short`

Expected: only the implementation and its tests are modified or untracked; no build artifacts are staged.
