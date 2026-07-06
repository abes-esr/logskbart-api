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
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.IntStream;


@Slf4j
@Service
public class LogsListener {
    private final EmailService emailService;
    @Value("${abes.in-memory.max-retention}")
    private long maxRetention;
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
    @KafkaListener(topics = {"${topic.name.source.logs}"}, groupId = "${topic.groupid.source}", containerFactory = "kafkaLogsListenerContainerFactory")
    public void listenInfoKbart2KafkaAndErrorKbart2Kafka(ConsumerRecord<String, String> message) throws IOException {
        LogKbartDto dto = mapper.readValue(message.value(), LogKbartDto.class);
        // recuperation de l'heure a laquelle le message a ete envoye
        String[] key = message.key().split(";");
        dto.setNbLine(Integer.parseInt(((key.length > 1) ? key[1] : "-1")));
        String packageName = key[0];
        if (!packageName.equals("${ctx:package}")) {
            traiterMessage(message, packageName, dto);
        }
    }

    private void traiterMessage(ConsumerRecord<String, String> message, String packageName, LogKbartDto dto) throws IOException {
        if (!this.workInProgressMap.containsKey(packageName)) {
            //vidage de la map des objets qui n'auraient pas été supprimés via le workflow normal et trop anciens
            freeObsoleteFromMap();
            //nouveau fichier trouvé dans le topic, on initialise les variables partagées
            log.debug("Nouveau package identifié : {} ", packageName);
            WorkInProgress workInProgress = new WorkInProgress();
            workInProgress.setTimestamp(new Timestamp(System.currentTimeMillis()));
            this.workInProgressMap.put(packageName, workInProgress);
        }
        LogKbart logKbart = logsMapper.map(dto, LogKbart.class);
        logKbart.setPackageName(packageName);
        logKbart.setTimestamp(new Date(message.timestamp()));
        this.workInProgressMap.get(packageName).addMessage(logKbart);

        if ((dto.getMessage().contains("Traitement terminé pour fichier " + packageName)) || (dto.getMessage().contains("Traitement refusé du fichier " + packageName))) {
            saveDatas(this.workInProgressMap.get(packageName).getMessages());
            if (!packageName.contains("_FORCE") || this.workInProgressMap.get(packageName).getMessages().stream().anyMatch(log ->
                    (log.getNbLine() == -1) && log.getMessage().contains("Format du fichier incorrect")
            )) {
                if (createFileBad(packageName)) {
                    emailService.sendEmail(packageName);
                    appendToCandidatsDoublons(packageName);
                    emailService.sendCandidatsDoublonsEmail(packageName);
                }
            }
            this.workInProgressMap.remove(packageName);
        }
    }

    public void freeObsoleteFromMap() {
        this.workInProgressMap.entrySet().removeIf(entry -> {
            long age = System.currentTimeMillis() - entry.getValue().getTimestamp().getTime();
            return age > this.maxRetention;
        });
    }

    private void saveDatas(List<LogKbart> logskbart) {
        //découpage de la liste en paquets de maxPacketSize pour sauvegarde dans ES pour éviter le timeout ou une erreur ES
        IntStream.range(0, (logskbart.size() + maxPacketSize - 1) / maxPacketSize)
                .mapToObj(i -> logskbart.subList(i * maxPacketSize, Math.min((i + 1) * maxPacketSize, logskbart.size())))
                .collect(Collectors.collectingAndThen(Collectors.toList(), Collections::synchronizedList))
                .forEach(logskbartList -> executor.execute(() -> {
                    log.debug("Saving logskbart : {}", logskbartList.size());
                    service.saveAll(logskbartList);
                }));
    }

    private void deleteOldLocalTempLog() throws IOException {
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

    private boolean createFileBad(String filename) throws IOException {
        log.debug("Entrée dans createFileBad : {}", filename);
        List<LogKbart> logskbartList = workInProgressMap.get(filename).getMessages().stream().filter(message -> message.getLevel().equals("ERROR")).sorted().toList();
        log.debug("Taille liste : {}", logskbartList.size());
        //List<LogKbart> logKbartList = service.getErrorLogKbartByPackageAndNbRun(filename, nbRun);
        Path tempPath = Path.of("tempLogLocal");
        if (!Files.exists(tempPath)) {
            Files.createDirectory(tempPath);
        }
        Path pathOfBadLocal = Path.of("tempLogLocal", filename.replace(".tsv", ".bad"));
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
                log.error("Erreur lors de la création du fichier temporaire. {}", e.getMessage());
                throw new RuntimeException(e);
            }
        });

        if (Files.exists(pathOfBadLocal)) {
            Path tempPathTarget = Path.of("tempLog");
            if (!Files.exists(tempPathTarget)) {
                Files.createDirectory(tempPathTarget);
            }
            //  Copie le fichier existant vers le répertoire temporaire
            Path pathOfBadFinal = Path.of("tempLog", filename.replace(".tsv", ".bad"));
            //  Déplacement du fichier
            Files.move(pathOfBadLocal, pathOfBadFinal, StandardCopyOption.REPLACE_EXISTING);
            log.info("Fichier de log transféré dans le dossier temporaire.");

            // Suppression du .log car Useless si cas là
            Path pathOfLog = Path.of("tempLog", filename.replace(".tsv", ".log"));
            log.info("Suppression de {}", pathOfLog);
            Files.deleteIfExists(pathOfLog);

            log.info("Suppression de {} en local", pathOfBadLocal);
            Files.deleteIfExists(pathOfBadLocal);
            return true;
        }
        return false;
    }

    /**
     * Lit le fichier .bad généré et transforme les erreurs structurées
     * pour les ajouter au fichier CandidatsDoublons.txt (concaténé)
     *
     * @param filename nom du fichier kbart traité
     * @throws IOException erreur d'accès au fichier
     */
    private void appendToCandidatsDoublons(String filename) throws IOException {
        Path badFile = Path.of("tempLog", filename.replace(".tsv", ".bad"));
        if (!Files.exists(badFile)) {
            log.debug("Fichier .bad non trouvé pour CandidatsDoublons : {}", badFile);
            return;
        }

        // Nom du bouquet : nom de fichier sans extension ni suffixes _FORCE/_BYPASS
        String bouquetName = filename.replace(".tsv", "").replaceAll("_(FORCE|BYPASS)$", "");

        List<String> lines = Files.readAllLines(badFile);
        StringBuilder linesToAdd = new StringBuilder();

        for (String line : lines) {
            // Sauter l'en-tête
            if (line.startsWith("LINE\tMESSAGE")) {
                continue;
            }

            // Extraire la partie message (après la tabulation)
            String[] parts = line.split("\t", 2);
            if (parts.length < 2) {
                continue;
            }
            String message = parts[1];

            // Ne traiter que les lignes contenant le format structuré
            if (!message.contains("publication title : ")) {
                continue;
            }

            // Extraction des champs via regex
            String natureErreur = extractField(message, "^(.+?)\\(");
            String ppns = extractField(message, "\\(([^)]+)\\)");
            String titre = extractField(message, "publication title : (.+?)(?: /| \\])");
            String typeRessource = extractField(message, "publication_type : (.+?)(?: /| \\])");
            String idOnline = extractField(message, "online_identifier : (.+?)(?: /| \\])");
            String idImprime = extractField(message, "print_identifier : (.+?)(?: /| \\])");

            // Construction de la requête WinIBW
            String requeteWinIBW = (ppns != null) ? "che ppn " + ppns : "";

            // Construction de la ligne de sortie
            linesToAdd.append(bouquetName).append("\t")
                    .append(natureErreur != null ? natureErreur.trim() : "").append("\t")
                    .append(requeteWinIBW).append("\t")
                    .append(titre != null ? titre.trim() : "").append("\t")
                    .append(typeRessource != null ? typeRessource.trim() : "").append("\t")
                    .append(idOnline != null ? idOnline.trim() : "").append("\t")
                    .append(idImprime != null ? idImprime.trim() : "")
                    .append(System.lineSeparator());
        }

        if (linesToAdd.isEmpty()) {
            log.debug("Aucune ligne à ajouter à CandidatsDoublons.txt pour {}", filename);
            return;
        }

        // Écriture/ajout au fichier CandidatsDoublons.txt
        Path candidatsDoublonsPath = Path.of("tempLog", "CandidatsDoublons.txt");
        if (!Files.exists(candidatsDoublonsPath)) {
            // Création du fichier avec en-tête
            String header = "Nom du bouquet\tNature de l'erreur\trequête WinIBW\tTitre\tType de ressource\tId online\tId imprimé" + System.lineSeparator();
            Files.write(candidatsDoublonsPath, (header + linesToAdd).getBytes());
        } else {
            Files.write(candidatsDoublonsPath, linesToAdd.toString().getBytes(), StandardOpenOption.APPEND);
        }

        log.info("Lignes ajoutées à CandidatsDoublons.txt pour le fichier {}", filename);
    }

    /**
     * Extrait un champ d'un message via une expression régulière
     *
     * @param message le message à analyser
     * @param regex   l'expression régulière (groupe 1 = valeur extraite)
     * @return la valeur extraite ou null si non trouvée
     */
    private String extractField(String message, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
}
