package fr.abes.logskbart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.logskbart.dto.LogKbartDto;
import fr.abes.logskbart.entity.LogKbart;
import fr.abes.logskbart.repository.LogKbartRepository;
import fr.abes.logskbart.service.EmailService;
import fr.abes.logskbart.utils.UtilsMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Date;

@Slf4j
@Service
public class LogsListener {

    private final ObjectMapper mapper;

    private final UtilsMapper logsMapper;

    private final LogKbartRepository repository;

    private final EmailService emailService;

    public LogsListener(ObjectMapper mapper, UtilsMapper logsMapper, LogKbartRepository repository, EmailService emailService) {
        this.mapper = mapper;
        this.logsMapper = logsMapper;
        this.repository = repository;
        this.emailService = emailService;
    }


    /**
     * Ecoute le topic de log d'erreurs et génère un fichier bad pour chaque fichier kbart
     *
     * @param message le message kafka
     * @throws IOException exception levée
     */
    @KafkaListener(topics = {"${topic.name.source.error}"}, groupId = "${topic.groupid.source}", containerFactory = "kafkaLogsListenerContainerFactory")
    public void listenInfoKbart2KafkaAndErrorKbart2Kafka(ConsumerRecord<String, String> message) throws IOException {
        LogKbartDto dto = mapper.readValue(message.value(), LogKbartDto.class);
        LogKbart logKbart = logsMapper.map(dto, LogKbart.class);

        String[] listMessage = message.key().split(";");
        log.debug(Arrays.toString(listMessage));
        // recuperation de l'heure a laquelle le message a ete envoye
        Timestamp currentTimestamp = new Timestamp(message.timestamp());
        logKbart.setTimestamp(new Date(currentTimestamp.getTime()));
        logKbart.setPackageName(listMessage[0]);
        String nbLineOrigine = (listMessage.length > 1) ? listMessage[1] : "";
        logKbart.setNbLine(Integer.parseInt((nbLineOrigine.isEmpty() ? "-1" : nbLineOrigine) ));

        logKbart.log();

        // Vérifie qu'un fichier portant le nom du kbart en cours existe
        if (!logKbart.getPackageName().contains("ctx:package") && !logKbart.getPackageName().contains("_FORCE")) {

            Path tempPath = Path.of("tempLogLocal");
            if(!Files.exists(tempPath)) {
                Files.createDirectory(tempPath);
            }
            Path of = Path.of("tempLogLocal" + File.separator + logKbart.getPackageName().replace(".tsv", ".bad"));

            //  Si la ligne de log sur le topic est de type ERROR
            if (logKbart.getLevel().toString().equals("ERROR")) {

                // vérifie la présence de fichiers obsolètes dans le répertoire tempLogLocal et les supprime le cas échéant
                deleteOldLocalTempLog();

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
            } else if (logKbart.getLevel().toString().equals("INFO")) {
                        // On verifie que le traitement commence pour supp les anciens logs du .bad (ps message venant de kbart2kafka)
                if (logKbart.getMessage().contains("Debut envois kafka de : " + logKbart.getPackageName())){
                    Files.deleteIfExists(of);
                        // On verifie que le traitement est terminé (ps message venant de best-ppn-api)
                }else if( logKbart.getMessage().contains("Traitement terminé pour fichier " + logKbart.getPackageName()) ) {
                    // Envoi du mail uniquement si le fichier temporaire a été créé
                    if (Files.exists(of)) {
                        Path tempPathTarget = Path.of("tempLog");
                        if (!Files.exists(tempPathTarget)) {
                            Files.createDirectory(tempPathTarget);
                        }
                        //  Copie le fichier existant vers le répertoire temporaire
                        Path target = Path.of("tempLog" + File.separator + logKbart.getPackageName().replace(".tsv", ".bad"));
                        //  Déplacement du fichier
                        Files.copy(of, target, StandardCopyOption.REPLACE_EXISTING);
                        log.info("Fichier de log transféré dans le dossier temporaire.");

                        emailService.sendMailWithAttachment(logKbart.getPackageName(), of);
                    }
                }
            }
        }
        //  Inscrit l'entity en BDD
        repository.save(logKbart);
    }

    public void deleteOldLocalTempLog() throws IOException {
        File dirToCheck = new File("tempLogLocal");
        File[] listeFilesTempLogLocal = dirToCheck.listFiles();
        if (listeFilesTempLogLocal != null) {
            for (File fileToCheck: listeFilesTempLogLocal) {
                BasicFileAttributes basicFileAttributes = Files.readAttributes(fileToCheck.toPath(), BasicFileAttributes.class);
                if (basicFileAttributes.isRegularFile()) {
                    String nameFile = String.valueOf(fileToCheck);
                    Date dateOfLastModification = new Date(basicFileAttributes.lastModifiedTime().toMillis());
                    Date dateNow = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
                    long interval = dateNow.getTime() - dateOfLastModification.getTime();
                    if (interval > 600000) {
                        Files.deleteIfExists(fileToCheck.toPath());
                        log.debug("Fichier obsolète supprimé : " + nameFile);
                    }
                }
            }
        }
    }
}
