package fr.abes.logskbart.repository.bacon;

import fr.abes.logskbart.configuration.BaconDbConfiguration;
import fr.abes.logskbart.entity.bacon.LigneKbart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@BaconDbConfiguration
public interface LigneKbartRepository extends JpaRepository<LigneKbart, Integer> {
}
