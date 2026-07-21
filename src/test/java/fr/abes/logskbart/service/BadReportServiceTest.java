package fr.abes.logskbart.service;

import fr.abes.logskbart.entity.LogKbart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BadReportServiceTest {

    @TempDir
    Path tempDir;

    private BadReportService service;

    @BeforeEach
    void setUp() {
        service = new BadReportService(tempDir.toString());
    }

    @Test
    @DisplayName("Sépare les erreurs 400 des autres erreurs")
    void write400AndOtherReportsSeparately() throws IOException {
        LogKbart validationError = error(12, "ISSN invalide");
        LogKbart duplicateError = error(34,
                "Plusieurs ppn trouvés [ publication title : Un titre / publication_type : serial ]");

        BadReportResult result = service.writeReports(
                "TEST_PROVIDER_PACKAGE_2026-07-16.tsv",
                List.of(duplicateError, validationError)
        );

        Path report400 = tempDir.resolve("bad/TEST_PROVIDER_PACKAGE_2026-07-16_400.bad");
        Path reportOther = tempDir.resolve("bad/TEST_PROVIDER_PACKAGE_2026-07-16_other.bad");
        assertTrue(result.has400Errors());
        assertTrue(result.hasOtherErrors());
        assertTrue(Files.readString(report400).contains("12\tISSN invalide"));
        assertFalse(Files.readString(report400).contains("publication title : "));
        assertTrue(Files.readString(reportOther).contains("34\tPlusieurs ppn trouvés"));
        assertTrue(Files.readString(reportOther).contains("publication title : Un titre"));
    }

    @Test
    @DisplayName("Ne crée pas de fichier other lorsque le traitement ne contient que des erreurs 400")
    void writeOnly400Report() throws IOException {
        BadReportResult result = service.writeReports(
                "TEST_PROVIDER_PACKAGE_2026-07-16.tsv",
                List.of(error(8, "Date de publication incorrecte"))
        );

        assertTrue(result.has400Errors());
        assertFalse(result.hasOtherErrors());
        assertTrue(Files.exists(tempDir.resolve("bad/TEST_PROVIDER_PACKAGE_2026-07-16_400.bad")));
        assertFalse(Files.exists(tempDir.resolve("bad/TEST_PROVIDER_PACKAGE_2026-07-16_other.bad")));
    }

    @Test
    @DisplayName("Ne crée pas de fichier 400 lorsque le traitement ne contient que d'autres erreurs")
    void writeOnlyOtherReport() throws IOException {
        BadReportResult result = service.writeReports(
                "TEST_PROVIDER_PACKAGE_2026-07-16.tsv",
                List.of(error(21, "Doublon [ publication title : Un titre ]"))
        );

        assertFalse(result.has400Errors());
        assertTrue(result.hasOtherErrors());
        assertFalse(Files.exists(tempDir.resolve("bad/TEST_PROVIDER_PACKAGE_2026-07-16_400.bad")));
        assertTrue(Files.exists(tempDir.resolve("bad/TEST_PROVIDER_PACKAGE_2026-07-16_other.bad")));
    }

    private LogKbart error(int lineNumber, String message) {
        LogKbart log = new LogKbart();
        log.setLevel("ERROR");
        log.setNbLine(lineNumber);
        log.setMessage(message);
        log.setTimestamp(new Date(lineNumber));
        return log;
    }
}
