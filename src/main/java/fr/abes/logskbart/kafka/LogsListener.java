package fr.abes.logskbart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.logskbart.dto.LogKbartDto;
import fr.abes.logskbart.entity.LogKbart;
import fr.abes.logskbart.repository.LogKbartRepository;
import fr.abes.logskbart.utils.UtilsMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class LogsListener {

    private final ObjectMapper mapper;

    private final UtilsMapper logsMapper;

    private final LogKbartRepository repository;

    private final Map<String, Timestamp> lastTimeStampByFilename;

    public LogsListener(ObjectMapper mapper, UtilsMapper logsMapper, LogKbartRepository repository, Map<String, Timestamp> lastTimeStampByFilename) {
        this.mapper = mapper;
        this.logsMapper = logsMapper;
        this.repository = repository;
        this.lastTimeStampByFilename = lastTimeStampByFilename;
    }


    /**
     * Ecoute les topic de log d'erreurs et de fin de traitement bestPpn et génère un fichier err pour chaque fichier kbart
     *
     * @param message le message kafka
     * @throws IOException exception levée
     */
    @KafkaListener(topics = {"${topic.name.source.error}", "${topic.name.source.info}"}, groupId = "${topic.groupid.source}", containerFactory = "kafkaLogsListenerContainerFactory")
    public void listenInfoKbart2KafkaAndErrorKbart2Kafka(ConsumerRecord<String, String> message) throws IOException {
        LogKbartDto dto = mapper.readValue(message.value(), LogKbartDto.class);
        LogKbart logKbart = logsMapper.map(dto, LogKbart.class);

        String[] listMessage = message.key().split(";");
        log.info(Arrays.toString(listMessage));
        // recuperation de l'heure a laquelle le message a ete envoye
        Timestamp currentTimestamp = new Timestamp(message.timestamp());
        logKbart.setTimestamp(new Date(currentTimestamp.getTime()));
        logKbart.setPackageName(listMessage[0]);
        String nbLineOrigine = (listMessage.length > 1) ? listMessage[1] : "";

        logKbart.log();

        // Vérifie qu'un fichier portant le nom du kbart en cours existe
        if (!logKbart.getPackageName().contains("ctx:package") && !logKbart.getPackageName().contains("_FORCE")) {
            Path tempPath = Path.of("tempLog");
            if(!Files.exists(tempPath)) {
                Files.createDirectory(tempPath);
            }
            Path of = Path.of("tempLog" + File.separator + logKbart.getPackageName().replace(".tsv", ".bad"));

            //  Si la ligne de log sur le topic errorkbart2kafka est de type ERROR
            if (logKbart.getLevel().toString().equals("ERROR")) {
                if (lastTimeStampByFilename.get(logKbart.getPackageName()) != null) {
                    Timestamp LastTimestampPlusTwoMinutes = new Timestamp(lastTimeStampByFilename.get(logKbart.getPackageName()).getTime() + TimeUnit.MINUTES.toMillis(2 * 60 * 1000));

                    // Si ca fait 2min qu'on a pas recu de message pour ce fichier
                    if (currentTimestamp.after(LastTimestampPlusTwoMinutes)) {
                        log.debug("Suppression fichier " + logKbart.getPackageName() + " si existe");
                        Files.deleteIfExists(of);
                    }
                }
                lastTimeStampByFilename.put(logKbart.getPackageName(), currentTimestamp);

                String line = nbLineOrigine + "\t" + logKbart.getMessage();

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
        }

        //  Inscrit l'entity en BDD
        repository.save(logKbart);
    }
}
