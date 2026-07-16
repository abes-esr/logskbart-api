package fr.abes.logskbart.service;

import fr.abes.logskbart.entity.LogKbart;
import fr.abes.logskbart.kafka.ErrorCategory;
import fr.abes.logskbart.kafka.ErrorClassifier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Slf4j
@Service
public class BadReportService {

    private static final String HEADER = "LINE\tMESSAGE\t" + System.lineSeparator();

    private final Path reportDirectory;
    private final Path badDirectory;

    public BadReportService(@Value("${abes.path-to-reports:tempLog}") String reportDirectory) {
        this.reportDirectory = Path.of(reportDirectory);
        this.badDirectory = this.reportDirectory.resolve("bad");
    }

    public BadReportResult writeReports(String filename, List<LogKbart> logs) throws IOException {
        Files.createDirectories(badDirectory);

        Path report400 = report400Path(filename);
        Path reportOther = otherReportPath(filename);
        Files.deleteIfExists(report400);
        Files.deleteIfExists(reportOther);

        List<LogKbart> errors = logs.stream()
                .filter(logKbart -> "ERROR".equals(logKbart.getLevel()))
                .sorted(Comparator
                        .comparing(LogKbart::getNbLine, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(LogKbart::getTimestamp, Comparator.nullsLast(Date::compareTo)))
                .toList();

        List<LogKbart> errors400 = errors.stream()
                .filter(error -> ErrorClassifier.classify(error.getMessage()) == ErrorCategory.ERREUR_400)
                .toList();
        List<LogKbart> otherErrors = errors.stream()
                .filter(error -> ErrorClassifier.classify(error.getMessage()) == ErrorCategory.AUTRE_ERREUR)
                .toList();

        writeReport(report400, errors400);
        writeReport(reportOther, otherErrors);

        return new BadReportResult(!errors400.isEmpty(), !otherErrors.isEmpty());
    }

    public Path reportDirectory() {
        return reportDirectory;
    }

    public Path report400Path(String filename) {
        return badDirectory.resolve(baseName(filename) + "_400.bad");
    }

    public Path otherReportPath(String filename) {
        return badDirectory.resolve(baseName(filename) + "_other.bad");
    }

    private void writeReport(Path path, List<LogKbart> errors) throws IOException {
        if (errors.isEmpty()) {
            return;
        }

        StringBuilder content = new StringBuilder(HEADER);
        errors.forEach(error -> content
                .append(error.getNbLine())
                .append('\t')
                .append(error.getMessage())
                .append(System.lineSeparator()));

        Files.writeString(
                path,
                content,
                StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING
        );
        log.info("Rapport d'erreurs créé : {}", path.toAbsolutePath());
    }

    private String baseName(String filename) {
        String safeFilename = Path.of(filename).getFileName().toString();
        return safeFilename.replaceFirst("(?i)\\.tsv$", "");
    }
}
