package fr.abes.logskbart.repository.bacon;

import fr.abes.logskbart.configuration.BaconDbConfiguration;
import fr.abes.logskbart.entity.bacon.ProviderPackage;
import fr.abes.logskbart.entity.bacon.ProviderPackageId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.Optional;

@Repository
@BaconDbConfiguration
public interface ProviderPackageRepository extends JpaRepository<ProviderPackage, ProviderPackageId> {
    Optional<ProviderPackage> findByProviderPackageId(ProviderPackageId providerPackageId);
}
