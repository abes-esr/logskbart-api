package fr.abes.logskbart.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class Daily400ReportFinderTest {

    @TempDir
    Path tempDir;

    @Test
    void findsOnly400ReportsInsideTheRequestedWindowSortedByName() throws IOException {
        Path badDirectory = Files.createDirectories(tempDir.resolve("bad"));
        Instant start = Instant.parse("2026-07-16T00:00:00Z");
        Instant cutoff = Instant.parse("2026-07-16T21:00:00Z");

        Path reportB = fileAt(badDirectory.resolve("B_400.bad"), cutoff);
        Path reportA = fileAt(badDirectory.resolve("A_400.bad"), start.plusSeconds(1));
        fileAt(badDirectory.resolve("OLD_400.bad"), start);
        fileAt(badDirectory.resolve("C_other.bad"), start.plusSeconds(2));
        Files.createDirectory(badDirectory.resolve("DIRECTORY_400.bad"));

        Daily400ReportFinder finder = new Daily400ReportFinder(tempDir.toString());

        assertEquals(List.of(reportA, reportB), finder.find(start, cutoff));
    }

    @Test
    void returnsAnEmptyListWhenBadDirectoryDoesNotExist() throws IOException {
        Daily400ReportFinder finder = new Daily400ReportFinder(tempDir.toString());

        assertEquals(List.of(), finder.find(Instant.EPOCH, Instant.now()));
    }

    private Path fileAt(Path path, Instant instant) throws IOException {
        Files.writeString(path, "report");
        Files.setLastModifiedTime(path, FileTime.from(instant));
        return path;
    }
}
