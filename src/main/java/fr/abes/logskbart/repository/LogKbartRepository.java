package fr.abes.logskbart.repository;

import fr.abes.logskbart.entity.LogKbart;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.stream.Stream;

@Repository
public interface LogKbartRepository extends ElasticsearchRepository<LogKbart, Long> {
    long countByPackageNameAndTimestampBetween(String packageName, Date debut, Date fin);

    Stream<LogKbart> findAllByPackageNameAndTimestampBetweenOrderByNbLineAscTimestampAsc(String filename, Date debut, Date fin);
}
