package fr.abes.logskbart.service;

import fr.abes.logskbart.entity.LogKbart;
import fr.abes.logskbart.exception.EmptyFileException;
import fr.abes.logskbart.repository.LogKbartRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Stream;

@Service
@Slf4j
public class LogsService {
    private final LogKbartRepository repository;

    public LogsService(LogKbartRepository logKbartRepository) {
        this.repository = logKbartRepository;
    }

    @Transactional(readOnly = true)
    public File getLogKbartForPackage(String packageName, Date date) throws IOException, EmptyFileException {
        Calendar dateChargement = Calendar.getInstance();
        dateChargement.setTime(date);
        Calendar dateFin = (Calendar) dateChargement.clone();
        dateFin.add(Calendar.DAY_OF_MONTH, 1);
        log.debug("packageName {}, Date début {}, Date fin {}", packageName, dateChargement.getTime(), dateFin.getTime());
        if (repository.countByPackageNameAndTimestampBetween(packageName, dateChargement.getTime(), dateFin.getTime()) == 0)
            throw new EmptyFileException("Aucun log pour le fichier " + packageName);
        Path tempPath = Path.of("tempLogLocal");
        if (!Files.exists(tempPath)) {
            Files.createDirectory(tempPath);
        }
        Path pathOfLocal = Path.of("tempLogLocal" + File.separator + packageName.replace(".tsv", ".log"));
        Files.deleteIfExists(pathOfLocal);
        try (Stream<LogKbart> logKbarts = repository.findAllByPackageNameAndTimestampBetweenOrderByNbLineAscTimestampAsc(packageName, dateChargement.getTime(), dateFin.getTime())) {
            logKbarts.forEach(logKbart -> {
                String message = (logKbart.getNbLine() != -1) ? logKbart.getNbLine() + " : " : "";
                message += logKbart.getMessage() + System.lineSeparator();
                try {
                    if (Files.exists(pathOfLocal)) {
                        Files.write(pathOfLocal, message.getBytes(), StandardOpenOption.APPEND);
                    } else {
                        //  Créer le fichier et inscrit la ligne dedans
                        Files.createFile(pathOfLocal);
                        //  Inscrit les informations sur la ligne
                        Files.write(pathOfLocal, message.getBytes(), StandardOpenOption.APPEND);
                        log.debug("Fichier temporaire créé.");
                    }
                }catch (IOException e) {
                    log.error("Impossible d'écrire dans le fichier local", e);
                }
            });
        }
        return pathOfLocal.toFile();
    }

    public void saveAll(List<LogKbart> logKbarts) {
        repository.saveAll(logKbarts);
        log.debug("Save done !");
    }
}
