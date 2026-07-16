package fr.abes.logskbart.configuration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ReportDirectoryInitializerTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Crée automatiquement le sous-répertoire bad au démarrage")
    void createBadDirectoryAtStartup() throws IOException {
        Path reportDirectory = tempDir.resolve("tempLog");
        ReportDirectoryInitializer initializer = new ReportDirectoryInitializer(reportDirectory.toString());

        initializer.initialize();

        assertTrue(Files.isDirectory(reportDirectory.resolve("bad")));
    }

    @Test
    @DisplayName("L'initialisation réussit lorsque le sous-répertoire bad existe déjà")
    void initializeExistingBadDirectory() throws IOException {
        Path reportDirectory = tempDir.resolve("tempLog");
        Files.createDirectories(reportDirectory.resolve("bad"));
        ReportDirectoryInitializer initializer = new ReportDirectoryInitializer(reportDirectory.toString());

        initializer.initialize();

        assertTrue(Files.isDirectory(reportDirectory.resolve("bad")));
    }
}
