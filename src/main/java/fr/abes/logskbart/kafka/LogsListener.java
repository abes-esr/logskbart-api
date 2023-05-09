package fr.abes.logskbart.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.logskbart.dto.Bacon2KafkaDto;
import fr.abes.logskbart.entity.LogKbart;
import fr.abes.logskbart.repository.LogKbartRepository;
import fr.abes.logskbart.utils.UtilsMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.sql.Timestamp;

@Service
public class LogsListener {

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UtilsMapper logsMapper;

    @Autowired
    private LogKbartRepository repository;

    @KafkaListener(topics = {"infobacon2kafka", "errorbacon2kafka"}, groupId = "logskbart")
    public void listenInfoBacon2Kafka(ConsumerRecord<String, String> message) throws JsonProcessingException {
        Bacon2KafkaDto dto = mapper.readValue(message.value(), Bacon2KafkaDto.class);
        LogKbart entity = logsMapper.map(dto, LogKbart.class);
        Timestamp timestamp = new Timestamp(message.timestamp());
        entity.setTimestamp(new Date(timestamp.getTime()));
        repository.save(entity);
    }
}
