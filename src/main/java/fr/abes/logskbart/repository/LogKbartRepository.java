package fr.abes.logskbart.repository;

import fr.abes.logskbart.entity.LogKbart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface LogKbartRepository extends JpaRepository<LogKbart, Long> {
    List<LogKbart> findAllByPackageNameAndTimestampBetween(String filename, Date debut, Date fin);
}
