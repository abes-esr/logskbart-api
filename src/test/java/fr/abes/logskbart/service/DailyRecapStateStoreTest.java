package fr.abes.logskbart.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DailyRecapStateStoreTest {

    @TempDir
    Path tempDir;

    @Test
    void returnsEmptyWhenNoSuccessfulRecapWasPersisted() throws IOException {
        DailyRecapStateStore store = new DailyRecapStateStore(tempDir.toString());

        assertEquals(Optional.empty(), store.read());
    }

    @Test
    void persistsTheLastSuccessfulRecapAcrossInstances() throws IOException {
        Instant successfulRecap = Instant.parse("2026-07-16T21:00:00Z");
        new DailyRecapStateStore(tempDir.toString()).write(successfulRecap);

        Optional<Instant> restored = new DailyRecapStateStore(tempDir.toString()).read();

        assertEquals(Optional.of(successfulRecap), restored);
    }

    @Test
    void rejectsAnInvalidPersistedInstant() throws IOException {
        Files.writeString(tempDir.resolve(".daily-400-recap-state"), "not-an-instant");
        DailyRecapStateStore store = new DailyRecapStateStore(tempDir.toString());

        assertThrows(IOException.class, store::read);
    }
}
