package fr.abes.logskbart.configuration;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Slf4j
@Component
public class ReportDirectoryInitializer {

    private final Path badDirectory;

    public ReportDirectoryInitializer(@Value("${abes.path-to-reports:tempLog}") String reportDirectory) {
        this.badDirectory = Path.of(reportDirectory, "bad");
    }

    @PostConstruct
    void initialize() throws IOException {
        Files.createDirectories(badDirectory);
        log.info("Répertoire de conservation des rapports .bad prêt : {}", badDirectory.toAbsolutePath());
    }
}
