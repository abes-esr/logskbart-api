package fr.abes.logskbart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

@SpringBootApplication
public class LogsKbartApplication {
    public static void main(String[] args) {
        SpringApplication.run(LogsKbartApplication.class, args);
    }
}
