package fr.abes.logskbart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.logskbart.dto.Kbart2KafkaDto;
import fr.abes.logskbart.entity.LogKbart;
import fr.abes.logskbart.repository.LogKbartRepository;
import fr.abes.logskbart.utils.UtilsMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.Date;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogsListener {

    @Value("${topic.name.source.error}")
    private String topicErrorKbart;

    @Value("${topic.name.source.info}")
    private String topicInfoKbart;

    @Value("${topic.name.source.endoftraitement}")
    private String topicEndOfTraitement;

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private UtilsMapper logsMapper;

    @Autowired
    private LogKbartRepository repository;

    /**
     * Ecoute les topic de log d'erreurs et de fin de traitement bestPpn et génère un fichier err pour chaque fichier kbart
     *
     * @param message le message kafka
     * @throws IOException exception levée
     */
    @KafkaListener(topics = {"${topic.name.source.error}", "${topic.name.source.info}"}, groupId = "logskbart", containerFactory = "kafkaLogsListenerContainerFactory")
    public void listenInfoKbart2KafkaAndErrorKbart2Kafka(ConsumerRecord<String, String> message) throws IOException {
        log.info("Error ou Info");
        Kbart2KafkaDto dto = mapper.readValue(message.value(), Kbart2KafkaDto.class);
        LogKbart entity = logsMapper.map(dto, LogKbart.class);
        Timestamp timestamp = new Timestamp(message.timestamp());
        entity.setTimestamp(new Date(timestamp.getTime()));
        entity.setPackageName(message.key().split("\\[")[0]);

        //  Si la ligne de log sur le topic errorkbart2kafka est de type ERROR
        if (entity.getLevel().toString().equals("ERROR")) {
            String[] listMessage = message.key().split("\\[line:");
            String nbrLine = (listMessage.length > 1) ? listMessage[1].replace("]", "") : "";
            String fileName = entity.getPackageName().replace(".tsv", ".bad");
            String line = nbrLine + "\t" + dto.getMessage();

            //  Vérifie qu'un fichier portant le nom du kbart en cours existe
            Path of = Path.of(fileName);
            if (Files.exists(of)) {
                //  Inscrit la ligne dedans
                Files.write(of, (line + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
            } else if (!Files.exists(of)) {
                try {
                    //  Créer le fichier et inscrit la ligne dedans
                    Files.createFile(of);
                    //  Créer la ligne d'en-tête
                    Files.write(of, ("LINE\tMESSAGE" + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
                    //  Inscrit les informations sur la ligne
                    Files.write(of, (line + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
                    log.info("Fichier temporaire créé.");
                } catch (SecurityException | IOException e) {
                    log.error("Erreur lors de la création du fichier temporaire. " + e);
                    throw new RuntimeException(e);
                }
            }
        }

        //  Inscrit l'entity en BDD
        repository.save(entity);
    }

    @KafkaListener(topics = {"bestppn.endoftraitment"}, groupId = "logskbart", containerFactory = "kafkaLogsListenerContainerFactory")
    public void listener(ConsumerRecord<String, String> message) throws IOException {
        log.info("End Of Traitement");
        //  Créer un nouveau Path avec le FileName (en remplaçant l'extension par .err)
        Path source = null;
        for (Header header : message.headers().toArray()) {
            if (header.key().equals("FileName")) {
                source = Path.of(new String(header.value()).replace(".tsv", ".bad"));
                break;
            }
        }

        //  Copie le fichier existant vers le répertoire temporaire en ajoutant sa date de création
        if (source != null && Files.exists(source)) {

            LocalDateTime time = LocalDateTime.now();
            DateTimeFormatter format = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss", Locale.FRANCE);
            String date = format.format(time);

            //  Vérification du chemin et création si inexistant
            String tempLog = "tempLog/";
            File chemin = new File(tempLog);
            if (!chemin.isDirectory()) {
                Files.createDirectory(Paths.get(tempLog));
            }
            Path target = Path.of("tempLog\\" + date + "_" + source);

            //  Déplacement du fichier
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            log.info("Fichier de log transféré dans le dossier temporaire.");
        }
    }
}
