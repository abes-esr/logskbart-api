package fr.abes.logskbart.repository;

import fr.abes.logskbart.entity.LogKbart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LogKbartRepository extends JpaRepository<LogKbart, Long> {
}
