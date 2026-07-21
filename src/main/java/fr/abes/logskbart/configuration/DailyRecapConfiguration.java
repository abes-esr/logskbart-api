package fr.abes.logskbart.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;
import java.time.ZoneId;

@Configuration(proxyBeanMethods = false)
@EnableScheduling
public class DailyRecapConfiguration {

    @Bean
    Clock dailyRecapClock(@Value("${abes.daily-recap.zone}") String zone) {
        return Clock.system(ZoneId.of(zone));
    }
}
