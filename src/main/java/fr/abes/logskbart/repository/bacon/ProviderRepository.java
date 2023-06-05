package fr.abes.logskbart.repository.bacon;

import fr.abes.logskbart.configuration.BaconDbConfiguration;
import fr.abes.logskbart.entity.bacon.Provider;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
@BaconDbConfiguration
public interface ProviderRepository extends JpaRepository<Provider, Integer> {
}
