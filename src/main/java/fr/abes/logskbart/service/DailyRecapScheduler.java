package fr.abes.logskbart.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.util.List;

@Slf4j
@Service
public class DailyRecapScheduler {

    private final Daily400ReportFinder finder;
    private final DailyRecapStateStore stateStore;
    private final EmailService emailService;
    private final Clock clock;

    public DailyRecapScheduler(Daily400ReportFinder finder,
                               DailyRecapStateStore stateStore,
                               EmailService emailService,
                               Clock clock) {
        this.finder = finder;
        this.stateStore = stateStore;
        this.emailService = emailService;
        this.clock = clock;
    }

    @Scheduled(cron = "${abes.daily-recap.cron}", zone = "${abes.daily-recap.zone}")
    public void scheduleDailyRecap() {
        try {
            runDailyRecap();
        } catch (IOException exception) {
            log.error("Impossible de générer le récapitulatif quotidien des erreurs 400", exception);
        }
    }

    void runDailyRecap() throws IOException {
        Instant cutoff = clock.instant();
        Instant previousCutoff = stateStore.read().orElseGet(() -> cutoff
                .atZone(clock.getZone())
                .toLocalDate()
                .atStartOfDay(clock.getZone())
                .toInstant());

        List<String> reportNames = finder.find(previousCutoff, cutoff).stream()
                .map(Path::getFileName)
                .map(Path::toString)
                .toList();

        if (reportNames.isEmpty()) {
            stateStore.write(cutoff);
            log.debug("Aucun rapport d'erreurs 400 à récapituler");
            return;
        }

        if (emailService.sendDailyRecapEmail(reportNames)) {
            stateStore.write(cutoff);
        } else {
            log.warn("Le récapitulatif quotidien n'a pas été envoyé ; les rapports seront repris ultérieurement");
        }
    }
}
