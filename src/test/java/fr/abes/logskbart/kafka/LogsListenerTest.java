package fr.abes.logskbart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.logskbart.service.BadReportResult;
import fr.abes.logskbart.service.BadReportService;
import fr.abes.logskbart.service.CandidatsDoublonsService;
import fr.abes.logskbart.service.EmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
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
    @DisplayName("Les autres erreurs déclenchent les emails immédiats si des candidats sont exportés")
    void sendsImmediateEmailsForOtherErrorsAndCandidates() throws IOException {
        String filename = "TEST_PROVIDER_PACKAGE_2025-11-02.tsv";
        when(candidatsDoublonsService.append(filename)).thenReturn(true);

        logsListener.notifyReports(filename, new BadReportResult(false, true));

        verify(emailService).sendOtherErrorsEmail(filename);
        verify(candidatsDoublonsService).append(filename);
        verify(emailService).sendCandidatsDoublonsEmail(filename);
    }

    @Test
    @DisplayName("L'email candidats n'est pas envoyé sans ligne structurée")
    void doesNotSendCandidatesEmailWhenNoCandidateWasExported() throws IOException {
        String filename = "TEST_PROVIDER_PACKAGE_2025-11-02.tsv";
        when(candidatsDoublonsService.append(filename)).thenReturn(false);

        logsListener.notifyReports(filename, new BadReportResult(false, true));

        verify(emailService).sendOtherErrorsEmail(filename);
        verify(candidatsDoublonsService).append(filename);
        verify(emailService, never()).sendCandidatsDoublonsEmail(anyString());
    }
}
