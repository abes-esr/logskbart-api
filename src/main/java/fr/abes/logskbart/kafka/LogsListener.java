package fr.abes.logskbart.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.logskbart.dto.LogKbartDto;
import fr.abes.logskbart.entity.LogKbart;
import fr.abes.logskbart.service.BadReportResult;
import fr.abes.logskbart.service.BadReportService;
import fr.abes.logskbart.service.CandidatsDoublonsService;
import fr.abes.logskbart.service.EmailService;
import fr.abes.logskbart.service.LogsService;
import fr.abes.logskbart.utils.UtilsMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Timestamp;
import java.util.*;
import java.util.concurrent.Executor;
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
    private final BadReportService badReportService;
    private final CandidatsDoublonsService candidatsDoublonsService;

    public LogsListener(ObjectMapper mapper, UtilsMapper logsMapper, LogsService service, Map<String, WorkInProgress> workInProgressMap, Executor executor, EmailService emailService, BadReportService badReportService, CandidatsDoublonsService candidatsDoublonsService) {
        this.mapper = mapper;
        this.logsMapper = logsMapper;
        this.service = service;
        this.workInProgressMap = workInProgressMap;
        this.executor = executor;
        this.emailService = emailService;
        this.badReportService = badReportService;
        this.candidatsDoublonsService = candidatsDoublonsService;
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
            notifyReports(packageName, createFileBad(packageName));
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

    private BadReportResult createFileBad(String filename) throws IOException {
        BadReportResult result = badReportService.writeReports(
                filename,
                workInProgressMap.get(filename).getMessages()
        );

        if (result.has400Errors() || result.hasOtherErrors()) {
            Path logFile = badReportService.reportDirectory().resolve(filename.replace(".tsv", ".log"));
            log.info("Suppression de {}", logFile);
            Files.deleteIfExists(logFile);
        }

        return result;
    }

    void notifyReports(String filename, BadReportResult reports) throws IOException {
        if (!reports.hasOtherErrors()) {
            return;
        }

        candidatsDoublonsService.append(filename);
    }
}
