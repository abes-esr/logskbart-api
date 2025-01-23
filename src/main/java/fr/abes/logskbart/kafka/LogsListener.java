package fr.abes.logskbart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.logskbart.dto.LogKbartDto;
import fr.abes.logskbart.entity.LogKbart;
import fr.abes.logskbart.service.EmailService;
import fr.abes.logskbart.service.LogsService;
import fr.abes.logskbart.utils.UtilsMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.stream.IntStream;


@Slf4j
@Service
public class LogsListener {
    private final EmailService emailService;
    @Value("${elasticsearch.max-packet-size}")
    private int maxPacketSize;

    private final ObjectMapper mapper;

    private final UtilsMapper logsMapper;

    private final LogsService service;

    private final Map<String, WorkInProgress> workInProgressMap;

    private final Executor executor;

    public LogsListener(ObjectMapper mapper, UtilsMapper logsMapper, LogsService service, Map<String, WorkInProgress> workInProgressMap, Executor executor, EmailService emailService) {
        this.mapper = mapper;
        this.logsMapper = logsMapper;
        this.service = service;
        this.workInProgressMap = workInProgressMap;
        this.executor = executor;
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
        // recuperation de l'heure a laquelle le message a ete envoye
        String[] key = message.key().split(";");
        dto.setNbLine(Integer.parseInt(((key.length > 1) ? key[1] : "-1")));
        String packageName = key[0];
        if (!packageName.equals("${ctx:package}")) {
            if (!this.workInProgressMap.containsKey(packageName)) {
                //nouveau fichier trouvé dans le topic, on initialise les variables partagées
                log.debug("Nouveau package identifié : " + packageName);
                workInProgressMap.put(packageName, new WorkInProgress());
            }
            LogKbart logKbart = logsMapper.map(dto, LogKbart.class);
            logKbart.setPackageName(packageName);
            logKbart.setTimestamp(new Date(message.timestamp()));
            workInProgressMap.get(packageName).addMessage(logKbart);

            if ((dto.getMessage().contains("Traitement terminé pour fichier " + packageName)) || (dto.getMessage().contains("Traitement refusé du fichier " + packageName))) {
                saveDatas(workInProgressMap.get(packageName).getMessages());
                if (!packageName.contains("_FORCE") || workInProgressMap.get(packageName).getMessages().stream().anyMatch(log ->
                        (log.getNbLine() == -1) && log.getMessage().equals("Format du fichier incorrect")
                )) {
                    createFileBad(packageName);
                    emailService.sendEmail(packageName);
                }
                workInProgressMap.remove(packageName);
            }
        }
    }

    private void saveDatas(List<LogKbart> logskbart) {
        //découpage de la liste en paquets de maxPacketSize pour sauvegarde dans ES pour éviter le timeout ou une erreur ES
        IntStream.range(0, (logskbart.size() + maxPacketSize - 1) / maxPacketSize)
                .mapToObj(i -> logskbart.subList(i * maxPacketSize, Math.min((i + 1) * maxPacketSize, logskbart.size())))
                .toList().forEach(logskbartList -> executor.execute(() -> {
                    log.debug("Saving logskbart : {}", logskbartList.size());
                    service.saveAll(logskbartList);
                }));
        log.debug("Sortie de la sauvegarde");
    }

    public void deleteOldLocalTempLog() throws IOException {
        File dirToCheck = new File("tempLogLocal");
        File[] listeFilesTempLogLocal = dirToCheck.listFiles();
        if (listeFilesTempLogLocal != null) {
            for (File fileToCheck : listeFilesTempLogLocal) {
                BasicFileAttributes basicFileAttributes = Files.readAttributes(fileToCheck.toPath(), BasicFileAttributes.class);
                if (basicFileAttributes.isRegularFile()) {
                    String nameFile = String.valueOf(fileToCheck);
                    Date dateOfLastModification = new Date(basicFileAttributes.lastModifiedTime().toMillis());
                    Date dateNow = Date.from(LocalDateTime.now().atZone(ZoneId.systemDefault()).toInstant());
                    long interval = dateNow.getTime() - dateOfLastModification.getTime();
                    if (interval > 600000 && Files.deleteIfExists(fileToCheck.toPath())) {
                        log.debug("Fichier obsolète supprimé : {}", nameFile);
                    }
                }
            }
        }
    }

    private void createFileBad(String filename) throws IOException {
        log.debug("Entrée dans createFileBad : {}", filename);
        List<LogKbart> logskbartList = workInProgressMap.get(filename).getMessages().stream().filter(message -> message.getLevel().equals("ERROR")).sorted().toList();
        log.debug("Taille liste : " + logskbartList.size());
        //List<LogKbart> logKbartList = service.getErrorLogKbartByPackageAndNbRun(filename, nbRun);
        Path tempPath = Path.of("tempLogLocal");
        if (!Files.exists(tempPath)) {
            Files.createDirectory(tempPath);
        }
        Path pathOfBadLocal = Path.of("tempLogLocal" + File.separator + filename.replace(".tsv", ".bad"));
        // vérifie la présence de fichiers obsolètes dans le répertoire tempLogLocal et les supprime le cas échéant
        deleteOldLocalTempLog();

        logskbartList.forEach(logKbart -> {
            try {
                if (Files.exists(pathOfBadLocal)) {
                    //  Inscrit la ligne dedans
                    Files.write(pathOfBadLocal, (logKbart.getNbLine() + "\t" + logKbart.getMessage() + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
                } else {
                    //  Créer le fichier et inscrit la ligne dedans
                    Files.createFile(pathOfBadLocal);
                    //  Créer la ligne d'en-tête
                    Files.write(pathOfBadLocal, ("LINE\tMESSAGE\t" + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
                    //  Inscrit les informations sur la ligne
                    Files.write(pathOfBadLocal, (logKbart.getNbLine() + "\t" + logKbart.getMessage() + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
                    log.info("Fichier temporaire créé.");
                }
            } catch (IOException e) {
                log.error("Erreur lors de la création du fichier temporaire. " + e);
                throw new RuntimeException(e);
            }
        });

        if (Files.exists(pathOfBadLocal)) {
            Path tempPathTarget = Path.of("tempLog");
            if (!Files.exists(tempPathTarget)) {
                Files.createDirectory(tempPathTarget);
            }
            //  Copie le fichier existant vers le répertoire temporaire
            Path pathOfBadFinal = Path.of("tempLog" + File.separator + filename.replace(".tsv", ".bad"));
            //  Déplacement du fichier
            Files.copy(pathOfBadLocal, pathOfBadFinal, StandardCopyOption.REPLACE_EXISTING);
            log.info("Fichier de log transféré dans le dossier temporaire.");

            // Suppression du .log car Useless si cas là
            Path pathOfLog = Path.of("tempLog" + File.separator + filename.replace(".tsv", ".log"));
            log.info("Suppression de " + pathOfLog);
            Files.deleteIfExists(pathOfLog);

            log.info("Suppression de " + pathOfBadLocal + " en local");
            Files.deleteIfExists(pathOfBadLocal);
        }
    }
}
