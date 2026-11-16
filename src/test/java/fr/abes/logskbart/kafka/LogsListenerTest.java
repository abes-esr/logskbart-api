package fr.abes.logskbart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.logskbart.service.BadReportResult;
import fr.abes.logskbart.service.BadReportService;
import fr.abes.logskbart.service.CandidatsDoublonsService;
import fr.abes.logskbart.service.EmailService;
import fr.abes.logskbart.service.LogsService;
import fr.abes.logskbart.utils.UtilsMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LogsListenerTest {

    private static final String BASE = "TEST_PROVIDER_PACKAGE_2025-11-02";
    private static final String INPUT_ERROR = "Identifiant ISSN invalide";
    private static final String OTHER_ERROR = "Plusieurs ppn électroniques (040651479 OU 040651525) ont le même score. "
            + "[ publication title : Journal Test Doublon SOA-503 ]";

    private LogsListener logsListener;
    private EmailService emailService;
    private CandidatsDoublonsService candidatsDoublonsService;

    @TempDir
    Path tempLogDir;

    static Stream<Arguments> errorCompletions() {
        return Stream.of("", "_FORCE", "_BYPASS").flatMap(mode ->
                Stream.of(false, true).flatMap(refused ->
                        Stream.of("400", "other", "mixed")
                                .map(category -> Arguments.of(mode, refused, category))));
    }

    static Stream<Arguments> missingOrEmptySummaries() {
        return Stream.of("", "_FORCE", "_BYPASS").flatMap(mode ->
                Stream.of(false, true).map(empty -> Arguments.of(mode, empty)));
    }

    static Stream<String> modes() {
        return Stream.of("", "_FORCE", "_BYPASS");
    }

    @ParameterizedTest(name = "synthèse intacte : mode={0}, refus={1}, erreurs={2}")
    @MethodSource("errorCompletions")
    void keepsExistingSummaryWhenErrorsAreReported(String mode, boolean refused, String category) throws IOException {
        Path summary = tempLogDir.resolve(BASE + mode + ".log");
        byte[] original = ("TOTAL LINES : 2\nLINES OK : 0\n"
                + "LINES WITH INPUT DATA ERRORS : 1\nLINES WITH ERRORS IN BESTPPN SEARCH : 1\n"
                + "FORCE_OPTION : " + "_FORCE".equals(mode) + "\n").getBytes(StandardCharsets.UTF_8);
        Files.write(summary, original);
        Path unrelated = tempLogDir.resolve("OTHER_PROVIDER_PACKAGE_2025-11-02.log");
        Files.writeString(unrelated, "Historique à conserver", StandardCharsets.UTF_8);

        completePackage(mode, refused, category);

        assertTrue(Files.exists(summary), "La synthèse doit survivre à la génération des rapports d'erreurs");
        assertArrayEquals(original, Files.readAllBytes(summary));
        assertEquals("Historique à conserver", Files.readString(unrelated, StandardCharsets.UTF_8));
    }

    @ParameterizedTest(name = "synthèse vide ou absente : mode={0}, vide={1}")
    @MethodSource("missingOrEmptySummaries")
    void doesNotInventOrDeleteAnEmptySummary(String mode, boolean empty) throws IOException {
        Path summary = tempLogDir.resolve(BASE + mode + ".log");
        if (empty) { Files.createFile(summary); }

        completePackage(mode, true, "mixed");

        assertEquals(empty, Files.exists(summary), "L'existence de la synthèse ne doit pas changer");
        if (empty) { assertEquals(0, Files.size(summary)); }
    }

    @ParameterizedTest
    @MethodSource("modes")
    void keepsSummaryWithoutErrors(String mode) throws IOException {
        Path summary = tempLogDir.resolve(BASE + mode + ".log");
        Files.writeString(summary, "Synthèse sans erreur", StandardCharsets.UTF_8);

        completePackage(mode, false, "none");

        assertEquals("Synthèse sans erreur", Files.readString(summary, StandardCharsets.UTF_8));
    }

    private void completePackage(String mode, boolean refused, String category) throws IOException {
        String filename = BASE + mode + ".tsv";
        Map<String, WorkInProgress> pending = new HashMap<>();
        LogsListener listener = new LogsListener(new ObjectMapper(), new UtilsMapper(),
                mock(LogsService.class), pending, Runnable::run, emailService,
                new BadReportService(tempLogDir.toString()), candidatsDoublonsService);
        ReflectionTestUtils.setField(listener, "maxPacketSize", 1000);
        ReflectionTestUtils.setField(listener, "maxRetention", 60_000L);
        boolean has400 = "400".equals(category) || "mixed".equals(category);
        boolean hasOther = "other".equals(category) || "mixed".equals(category);
        if (has400) { sendMessage(listener, filename + ";1", "ERROR", INPUT_ERROR); }
        if (hasOther) { sendMessage(listener, filename + ";2", "ERROR", OTHER_ERROR); }
        sendMessage(listener, filename, "INFO", (refused ? "Traitement refusé du fichier "
                : "Traitement terminé pour fichier ") + filename);

        Path report400 = tempLogDir.resolve("bad").resolve(BASE + mode + "_400.bad");
        Path reportOther = tempLogDir.resolve("bad").resolve(BASE + mode + "_other.bad");
        assertEquals(has400, Files.exists(report400));
        assertEquals(hasOther, Files.exists(reportOther));
        if (has400) {
            String content = Files.readString(report400, StandardCharsets.UTF_8);
            assertTrue(content.contains("1\t" + INPUT_ERROR));
            assertFalse(content.contains(OTHER_ERROR));
        }
        if (hasOther) {
            String content = Files.readString(reportOther, StandardCharsets.UTF_8);
            assertTrue(content.contains("2\t" + OTHER_ERROR));
            assertFalse(content.contains(INPUT_ERROR));
            verify(candidatsDoublonsService).append(filename);
        } else { verifyNoInteractions(candidatsDoublonsService); }
        verifyNoInteractions(emailService);
        assertTrue(pending.isEmpty(), "Le cycle du bouquet doit être terminé");
    }

    private void sendMessage(LogsListener listener, String key, String level, String message) throws IOException {
        listener.listenInfoKbart2KafkaAndErrorKbart2Kafka(new ConsumerRecord<>("logs", 0, 0L, key,
                new ObjectMapper().writeValueAsString(Map.of("level", level, "message", message))));
    }

    @BeforeEach
    void setUp() {
        emailService = mock(EmailService.class);
        candidatsDoublonsService = mock(CandidatsDoublonsService.class);

        logsListener = new LogsListener(
                new ObjectMapper(),
                null,
                null,
                Map.of(),
                null,
                emailService,
                new BadReportService(tempLogDir.toString()),
                candidatsDoublonsService
        );
    }

    @Test
    @DisplayName("Aucun email immédiat n'est envoyé pour des erreurs 400 seules")
    void doesNotSendImmediateEmailFor400Only() throws IOException {
        logsListener.notifyReports(
                "TEST_PROVIDER_PACKAGE_2025-11-02.tsv",
                new BadReportResult(true, false)
        );

        verifyNoInteractions(emailService, candidatsDoublonsService);
    }

    @Test
    @DisplayName("Les candidats sont exportés sans déclencher d'email dédié")
    void exportsCandidatesWithoutSendingDedicatedEmail() throws IOException {
        String filename = "TEST_PROVIDER_PACKAGE_2025-11-02.tsv";
        when(candidatsDoublonsService.append(filename)).thenReturn(true);

        logsListener.notifyReports(filename, new BadReportResult(false, true));

        verifyNoInteractions(emailService);
        verify(candidatsDoublonsService).append(filename);
    }

    @Test
    @DisplayName("L'email candidats n'est pas envoyé sans ligne structurée")
    void doesNotSendCandidatesEmailWhenNoCandidateWasExported() throws IOException {
        String filename = "TEST_PROVIDER_PACKAGE_2025-11-02.tsv";
        when(candidatsDoublonsService.append(filename)).thenReturn(false);

        logsListener.notifyReports(filename, new BadReportResult(false, true));

        verifyNoInteractions(emailService);
        verify(candidatsDoublonsService).append(filename);
    }

    @Test
    @DisplayName("Un fichier FORCE produit les rapports des autres erreurs")
    void forceFileProducesOtherErrorsReports() throws IOException {
        String filename = "TEST_PROVIDER_PACKAGE_2025-11-02_FORCE.tsv";
        LogsService logsService = mock(LogsService.class);
        when(candidatsDoublonsService.append(filename)).thenReturn(true);

        logsListener = new LogsListener(
                new ObjectMapper(),
                new UtilsMapper(),
                logsService,
                new HashMap<>(),
                Runnable::run,
                emailService,
                new BadReportService(tempLogDir.toString()),
                candidatsDoublonsService
        );
        ReflectionTestUtils.setField(logsListener, "maxPacketSize", 1000);

        String duplicateError = "Plusieurs ppn électroniques (040651479 OU 040651525) ont le même score. "
                + "[ publication title : Journal Test Doublon SOA-503 ]";
        logsListener.listenInfoKbart2KafkaAndErrorKbart2Kafka(new ConsumerRecord<>(
                "logs", 0, 0L, filename + ";1",
                "{\"level\":\"ERROR\",\"message\":\"" + duplicateError + "\"}"
        ));
        logsListener.listenInfoKbart2KafkaAndErrorKbart2Kafka(new ConsumerRecord<>(
                "logs", 0, 1L, filename,
                "{\"level\":\"INFO\",\"message\":\"Traitement terminé pour fichier " + filename + "\"}"
        ));

        Path otherReport = tempLogDir.resolve("bad")
                .resolve("TEST_PROVIDER_PACKAGE_2025-11-02_FORCE_other.bad");
        org.junit.jupiter.api.Assertions.assertTrue(Files.exists(otherReport));
        verifyNoInteractions(emailService);
        verify(candidatsDoublonsService).append(filename);
    }
}
