package fr.abes.logskbart.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

@Service
public class Daily400ReportFinder {

    private final Path badDirectory;

    public Daily400ReportFinder(@Value("${abes.path-to-reports:tempLog}") String reportDirectory) {
        this.badDirectory = Path.of(reportDirectory).resolve("bad");
    }

    public List<Path> find(Instant afterExclusive, Instant upToInclusive) throws IOException {
        if (!Files.isDirectory(badDirectory)) {
            return List.of();
        }

        try (Stream<Path> files = Files.list(badDirectory)) {
            List<Path> candidates = files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith("_400.bad"))
                    .toList();

            List<Path> reports = new ArrayList<>();
            for (Path candidate : candidates) {
                Instant modifiedAt = Files.getLastModifiedTime(candidate).toInstant();
                if (modifiedAt.isAfter(afterExclusive) && !modifiedAt.isAfter(upToInclusive)) {
                    reports.add(candidate);
                }
            }
            reports.sort(Comparator.comparing(path -> path.getFileName().toString()));
            return List.copyOf(reports);
        }
    }
}
