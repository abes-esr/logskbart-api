package fr.abes.logskbart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.logskbart.dto.Kbart2KafkaDto;
import fr.abes.logskbart.entity.LogKbart;
import fr.abes.logskbart.repository.LogKbartRepository;
import fr.abes.logskbart.utils.UtilsMapper;
import lombok.RequiredArgsConstructor;
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
import java.util.Arrays;

@Slf4j
@Service
@RequiredArgsConstructor
public class LogsListener {

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
    @KafkaListener(topics = {"${topic.name.source.error}", "${topic.name.source.info}"}, groupId = "${topic.groupid.source}", containerFactory = "kafkaLogsListenerContainerFactory")
    public void listenInfoKbart2KafkaAndErrorKbart2Kafka(ConsumerRecord<String, String> message) throws IOException {
        Kbart2KafkaDto dto = mapper.readValue(message.value(), Kbart2KafkaDto.class);
        LogKbart logKbart = logsMapper.map(dto, LogKbart.class);

        String[] listMessage = message.key().split(";");
        log.info(Arrays.toString(listMessage));
        Timestamp timestamp = new Timestamp(message.timestamp());
        logKbart.setTimestamp(new Date(timestamp.getTime()));
        logKbart.setPackageName(listMessage[0]);

        logKbart.log();

        if (!logKbart.getPackageName().contains("ctx:package")) {
            //  Si la ligne de log sur le topic errorkbart2kafka est de type ERROR
            if (logKbart.getLevel().toString().equals("ERROR")) {
                String nbrLine = (listMessage.length > 1) ? listMessage[1] : "";
                String fileName = logKbart.getPackageName().replace(".tsv", ".bad");
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
        }

        //  Inscrit l'entity en BDD
        repository.save(logKbart);
    }

    @KafkaListener(topics = {"${topic.name.source.endoftraitement}"}, groupId = "${topic.groupid.source}", containerFactory = "kafkaLogsListenerContainerFactory")
    public void listener(ConsumerRecord<String, String> message) throws IOException {

        //  Créer un nouveau Path avec le FileName (en remplaçant l'extension par .bad)
        Path source = null;
        for (Header header : message.headers().toArray()) {
            if (header.key().equals("FileName")) {
                source = Path.of(new String(header.value()).replace(".tsv", ".bad"));
                break;
            }
        }

        log.info("End of traitement : " + message.value());

        if( message.value().equals("OK")) {
            //  Copie le fichier existant vers le répertoire temporaire en ajoutant sa date de création
            if (source != null && Files.exists(source)) {


                //  Vérification du chemin et création si inexistant
                String tempLog = "tempLog" + File.separator;
                File chemin = new File(tempLog);
                if (!chemin.isDirectory()) {
                    Files.createDirectory(Paths.get(tempLog));
                }
                Path target = Path.of(tempLog + source);
                Files.deleteIfExists(target);

                //  Déplacement du fichier
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
                log.info("Fichier de log d'erreur transféré dans le dossier temporaire.");
            }
        } else if(message.value().equals("KO")) {
            assert source != null;
            Files.deleteIfExists(source);
            log.info("Fichier de log d'erreur supprimé si existe");
        }
    }
}
