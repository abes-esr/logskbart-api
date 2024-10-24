package fr.abes.logskbart.configuration;

import fr.abes.logskbart.kafka.WorkInProgress;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableKafka
public class KafkaConfig {
    @Value("${abes.kafka.bootstrap-servers}")
    private String bootstrapAddress;
    @Value("${abes.nbThread}")
    private int nbThread;


    @Bean
    public ConsumerFactory<String, String> consumerLogsFactory() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapAddress);
        props.put(ConsumerConfig.GROUP_INSTANCE_ID_CONFIG,("SchedulerCoordinator"+ UUID.randomUUID()));
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);
        props.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        props.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 60000);
        return new DefaultKafkaConsumerFactory<>(props);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, String>
    kafkaLogsListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerLogsFactory());
        return factory;
    }

    @Bean
    public Map<String, WorkInProgress> workInProgressMap() {
        return new ConcurrentHashMap<>();
    }

    @Bean
    public Executor executor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(nbThread);
        executor.setMaxPoolSize(nbThread);
        executor.setQueueCapacity(500);
        executor.initialize();
        return executor;
    }
}
