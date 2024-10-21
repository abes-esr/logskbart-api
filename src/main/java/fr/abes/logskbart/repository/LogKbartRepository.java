package fr.abes.logskbart.repository;

import fr.abes.logskbart.entity.LogKbart;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface LogKbartRepository extends ElasticsearchRepository<LogKbart, Long> {
    List<LogKbart> findAllByPackageNameAndTimestampBetweenOrderByNbLineAscTimestampAsc(String filename, Date debut, Date fin);

    List<LogKbart> findAllByPackageNameAndNbRunAndLevelOrderByNbLineAscTimestampAsc(String filename, Integer nbRun, String level);

    List<LogKbart> findByPackageNameOrderByNbRunDesc(String filename);
}
