package fr.abes.logskbart.service;

import fr.abes.logskbart.entity.LogKbart;
import fr.abes.logskbart.repository.LogKbartRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;
import java.util.List;

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

    public void saveAll(List<LogKbart> logKbarts) {
        repository.saveAll(logKbarts);
        log.debug("Save done !");
    }

    public void save(LogKbart logKbart) {
        repository.save(logKbart);
    }
}
