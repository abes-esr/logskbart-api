package fr.abes.logskbart.service;

import fr.abes.logskbart.entity.LogKbart;
import fr.abes.logskbart.repository.LogKbartRepository;
import fr.abes.logskbart.utils.Level;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class LogsService {
    private final LogKbartRepository repository;

    public LogsService(LogKbartRepository repository) {
        this.repository = repository;
    }

    public List<LogKbart> getLogKbartForPackage(String packageName, Date date) {
        Calendar dateChargement = Calendar.getInstance();
        dateChargement.setTime(date);
        Calendar dateFin = (Calendar) dateChargement.clone();
        dateFin.add(Calendar.DAY_OF_MONTH, 1);
        log.debug("packageName {}, Date début {}, Date fin {}", packageName, dateChargement.getTime(), dateFin.getTime());
        return repository.findAllByPackageNameAndTimestampBetweenOrderByNbLineAscTimestampAsc(packageName, dateChargement.getTime(), dateFin.getTime());
    }

    public LogKbart save(LogKbart logKbart) {
        return repository.save(logKbart);
    }

    public Integer getLastNbRun(String packageName) {
        Optional<LogKbart> logKbart = repository.getFirstByPackageNameOrderByNbRunDesc(packageName);
        if(logKbart.isPresent()) {
            return logKbart.get().getNbRun();
        } else {
            return 0;
        }
    }

    public List<LogKbart> getErrorLogKbartByPackageAndNbRun(String packageName, Integer nbRun) {
        return repository.findAllByPackageNameAndNbRunAndLevelOrderByNbLineAscTimestampAsc(packageName,nbRun, Level.ERROR);
    }
}
