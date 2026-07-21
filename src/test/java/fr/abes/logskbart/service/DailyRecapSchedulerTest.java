package fr.abes.logskbart.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyRecapSchedulerTest {

    private static final Instant CUTOFF = Instant.parse("2026-07-16T21:00:00Z");
    private static final ZoneId PARIS = ZoneId.of("Europe/Paris");

    @Mock
    Daily400ReportFinder finder;
    @Mock
    DailyRecapStateStore stateStore;
    @Mock
    EmailService emailService;

    private DailyRecapScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new DailyRecapScheduler(
                finder,
                stateStore,
                emailService,
                Clock.fixed(CUTOFF, PARIS)
        );
    }

    @Test
    void startsAtTheBeginningOfTheCurrentParisDayWhenNoStateExists() throws IOException {
        Instant startOfDay = Instant.parse("2026-07-15T22:00:00Z");
        when(stateStore.read()).thenReturn(Optional.empty());
        when(finder.find(startOfDay, CUTOFF)).thenReturn(List.of());

        scheduler.runDailyRecap();

        verify(finder).find(startOfDay, CUTOFF);
        verify(emailService, never()).sendDailyRecapEmail(List.of());
        verify(stateStore).write(CUTOFF);
    }

    @Test
    void sendsOneEmailAndAdvancesStateAfterSuccess() throws IOException {
        Instant previousRun = Instant.parse("2026-07-15T21:00:00Z");
        List<Path> reports = List.of(Path.of("bad/A_400.bad"), Path.of("bad/B_400.bad"));
        when(stateStore.read()).thenReturn(Optional.of(previousRun));
        when(finder.find(previousRun, CUTOFF)).thenReturn(reports);
        when(emailService.sendDailyRecapEmail(List.of("A_400.bad", "B_400.bad"))).thenReturn(true);

        scheduler.runDailyRecap();

        verify(emailService).sendDailyRecapEmail(List.of("A_400.bad", "B_400.bad"));
        verify(stateStore).write(CUTOFF);
    }

    @Test
    void keepsPreviousStateWhenEmailFails() throws IOException {
        Instant previousRun = Instant.parse("2026-07-15T21:00:00Z");
        when(stateStore.read()).thenReturn(Optional.of(previousRun));
        when(finder.find(previousRun, CUTOFF)).thenReturn(List.of(Path.of("bad/A_400.bad")));
        when(emailService.sendDailyRecapEmail(List.of("A_400.bad"))).thenReturn(false);

        scheduler.runDailyRecap();

        verify(stateStore, never()).write(CUTOFF);
    }
}
