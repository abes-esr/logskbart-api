package fr.abes.logskbart.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.time.Clock;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DailyRecapConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(DailyRecapConfiguration.class)
            .withPropertyValues("abes.daily-recap.zone=Europe/Paris");

    @Test
    void configuresTheDailyRecapClockInParisTimeZone() {
        contextRunner.run(context -> {
            Clock clock = context.getBean(Clock.class);

            assertEquals(ZoneId.of("Europe/Paris"), clock.getZone());
        });
    }
}
