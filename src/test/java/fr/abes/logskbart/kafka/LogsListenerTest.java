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
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

class LogsListenerTest {

    private LogsListener logsListener;
    private EmailService emailService;
    private CandidatsDoublonsService candidatsDoublonsService;

    @TempDir
    Path tempLogDir;

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

        verify(emailService).sendOtherErrorsEmail(filename);
        verify(candidatsDoublonsService).append(filename);
        verifyNoMoreInteractions(emailService);
    }

    @Test
    @DisplayName("L'email candidats n'est pas envoyé sans ligne structurée")
    void doesNotSendCandidatesEmailWhenNoCandidateWasExported() throws IOException {
        String filename = "TEST_PROVIDER_PACKAGE_2025-11-02.tsv";
        when(candidatsDoublonsService.append(filename)).thenReturn(false);

        logsListener.notifyReports(filename, new BadReportResult(false, true));

        verify(emailService).sendOtherErrorsEmail(filename);
        verify(candidatsDoublonsService).append(filename);
        verifyNoMoreInteractions(emailService);
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
        verify(emailService).sendOtherErrorsEmail(filename);
        verify(candidatsDoublonsService).append(filename);
        verifyNoMoreInteractions(emailService);
    }
}
