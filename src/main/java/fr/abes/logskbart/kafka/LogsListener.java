package fr.abes.logskbart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.logskbart.dto.LogKbartDto;
import fr.abes.logskbart.entity.LogKbart;
import fr.abes.logskbart.service.EmailService;
import fr.abes.logskbart.service.LogsService;
import fr.abes.logskbart.utils.UtilsMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
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


@Slf4j
@Service
public class LogsListener {

    private final ObjectMapper mapper;

    private final UtilsMapper logsMapper;

    private final LogsService service;

    private final EmailService emailService;

    private final Map<String, WorkInProgress> workInProgressMap;

    public LogsListener(ObjectMapper mapper, UtilsMapper logsMapper, LogsService service, EmailService emailService, Map<String, WorkInProgress> workInProgressMap) {
        this.mapper = mapper;
        this.logsMapper = logsMapper;
        this.service = service;
        this.emailService = emailService;
        this.workInProgressMap = workInProgressMap;
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
        String packageName = key[0];
        if (!this.workInProgressMap.containsKey(packageName)) {
            //nouveau fichier trouvé dans le topic, on initialise les variables partagées
            log.debug("Nouveau package identifié : " + packageName);
            workInProgressMap.put(packageName, new WorkInProgress());
        }
        workInProgressMap.get(packageName).addMessage(dto);
        if (!packageName.contains("ctx:package") && !packageName.contains("_FORCE")) {
            if ((dto.getMessage().contains("Traitement terminé pour fichier " + packageName)) || (dto.getMessage().contains("Traitement refusé du fichier " + packageName))) {
                log.debug("Commit les datas pour fichier " + packageName);
                Integer nbLine = Integer.parseInt(((key.length > 1) ? key[1] : "-1"));
                Integer nbRun = commitDatas(message.timestamp(), packageName, nbLine);
                createFileBad(packageName, nbRun);
                workInProgressMap.remove(packageName);
            }
        }
    }

    private Integer commitDatas(long timeStamp, String packageName, Integer nbLine) {
        long startTime = System.currentTimeMillis();
        log.debug("Debut Commit datas pour fichier " + packageName);
        List<LogKbart> logskbart = logsMapper.mapList(workInProgressMap.get(packageName).getMessages(), LogKbart.class);
        int nbRun = service.getLastNbRun(packageName) + 1;
        log.debug("NbRun: " + nbRun);
        saveDatas(timeStamp, packageName, nbLine, logskbart, nbRun);
        log.debug("datas saved pour fichier " + packageName);
        long endTime = System.currentTimeMillis();
        double executionTime = (double) (endTime - startTime) / 1000;
        log.debug("Execution time: " + executionTime);
        return nbRun;
    }

    private void saveDatas(long timeStamp, String packageName, Integer nbLine, List<LogKbart> logskbart, int nbRun) {
        logskbart.forEach(logKbart -> {
            logKbart.setNbRun(nbRun);
            logKbart.setTimestamp(new Date(timeStamp));
            logKbart.setPackageName(packageName);
            logKbart.setNbLine(nbLine);
        });
        service.saveAll(logskbart);
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

    private void createFileBad(String filename, Integer nbRun) throws IOException {
        List<LogKbart> logKbartList = service.getErrorLogKbartByPackageAndNbRun(filename, nbRun);
        Path tempPath = Path.of("tempLogLocal");
        if (!Files.exists(tempPath)) {
            Files.createDirectory(tempPath);
        }
        Path pathOfBadLocal = Path.of("tempLogLocal" + File.separator + filename.replace(".tsv", ".bad"));
        // vérifie la présence de fichiers obsolètes dans le répertoire tempLogLocal et les supprime le cas échéant
        deleteOldLocalTempLog();

        logKbartList.forEach(logKbart -> {
            try {
                if (Files.exists(pathOfBadLocal)) {
                    //  Inscrit la ligne dedans
                    Files.write(pathOfBadLocal, (logKbart.getNbLine() + "\t" + logKbart.getMessage() + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
                } else {
                    //  Créer le fichier et inscrit la ligne dedans
                    Files.createFile(pathOfBadLocal);
                    //  Créer la ligne d'en-tête
                    Files.write(pathOfBadLocal, ("LINE\tMESSAGE\t(" + nbRun + ")" + System.lineSeparator()).getBytes(), StandardOpenOption.APPEND);
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
            long tailleDixMo = 10 * 1024 * 1024;
            if (pathOfBadFinal.toFile().length() < tailleDixMo) {
                emailService.sendMailWithAttachment(filename, pathOfBadLocal);
            } else {
                emailService.sendEmail(filename, "Le fichier est trop volumineux, retrouvez le sur le chemin : /applis/bacon/toLoad/" + filename.replace(".tsv", ".bad"));
            }

            log.info("Suppression de " + pathOfBadLocal + " en local");
            Files.deleteIfExists(pathOfBadLocal);
        }
    }
}
