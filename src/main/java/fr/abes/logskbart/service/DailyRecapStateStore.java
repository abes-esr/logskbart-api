package fr.abes.logskbart.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Optional;

@Service
public class DailyRecapStateStore {

    private static final String STATE_FILENAME = ".daily-400-recap-state";

    private final Path reportDirectory;
    private final Path stateFile;

    public DailyRecapStateStore(@Value("${abes.path-to-reports:tempLog}") String reportDirectory) {
        this.reportDirectory = Path.of(reportDirectory);
        this.stateFile = this.reportDirectory.resolve(STATE_FILENAME);
    }

    public Optional<Instant> read() throws IOException {
        if (!Files.exists(stateFile)) {
            return Optional.empty();
        }

        try {
            return Optional.of(Instant.parse(Files.readString(stateFile, StandardCharsets.UTF_8).trim()));
        } catch (DateTimeParseException exception) {
            throw new IOException("État du récapitulatif quotidien invalide : " + stateFile, exception);
        }
    }

    public void write(Instant instant) throws IOException {
        Files.createDirectories(reportDirectory);
        Path temporaryFile = reportDirectory.resolve(STATE_FILENAME + ".tmp");
        Files.writeString(temporaryFile, instant.toString(), StandardCharsets.UTF_8);

        try {
            Files.move(
                    temporaryFile,
                    stateFile,
                    StandardCopyOption.ATOMIC_MOVE,
                    StandardCopyOption.REPLACE_EXISTING
            );
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(temporaryFile, stateFile, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
