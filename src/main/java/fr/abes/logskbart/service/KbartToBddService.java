package fr.abes.logskbart.service;

import fr.abes.logskbart.dto.PackageKbartDto;
import fr.abes.logskbart.entity.bacon.LigneKbart;
import fr.abes.logskbart.entity.bacon.Provider;
import fr.abes.logskbart.entity.bacon.ProviderPackage;
import fr.abes.logskbart.entity.bacon.ProviderPackageId;
import fr.abes.logskbart.repository.bacon.LigneKbartRepository;
import fr.abes.logskbart.repository.bacon.ProviderPackageRepository;
import fr.abes.logskbart.repository.bacon.ProviderRepository;
import fr.abes.logskbart.utils.UtilsMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class KbartToBddService implements KbartLoader {
    private final ProviderPackageRepository providerPackageRepository;
    private final ProviderRepository providerRepository;
    private final LigneKbartRepository ligneKbartRepository;

    private final UtilsMapper mapper;

    public KbartToBddService(ProviderPackageRepository providerPackageRepository, ProviderRepository providerRepository, LigneKbartRepository ligneKbartRepository, UtilsMapper mapper) {
        this.providerPackageRepository = providerPackageRepository;
        this.providerRepository = providerRepository;
        this.ligneKbartRepository = ligneKbartRepository;
        this.mapper = mapper;
    }

    @Override
    public void chargerPackageKbart(PackageKbartDto packageKbartDto) {
        ProviderPackage providerPackage = handlePackageAndProvider(packageKbartDto);
        List<LigneKbart> ligneKbarts = mapper.mapList(packageKbartDto.getLigneKbartDtos(), LigneKbart.class);
        ligneKbarts.forEach(ligne -> ligne.setProviderPackage(providerPackage));
        ligneKbartRepository.saveAll(ligneKbarts);
    }

    private ProviderPackage handlePackageAndProvider(PackageKbartDto packageKbartDto) {
        //on vérifie si le package existe dans la base pour une date de chargement donnée
        //récupération provider
        Optional<Provider> providerOpt = providerRepository.findByProvider(packageKbartDto.getProvider());
        if (providerOpt.isPresent()) {
            Provider provider = providerOpt.get();
            ProviderPackageId providerPackageId = new ProviderPackageId(packageKbartDto.getPackageName(), packageKbartDto.getDatePackage(), provider.getIdtProvider());
            Optional<ProviderPackage> providerPackage = providerPackageRepository.findByProviderPackageId(providerPackageId);
            //pas d'info de package, on le crée
            return providerPackage.orElseGet(() -> providerPackageRepository.save(new ProviderPackage(providerPackageId)));
        } else {
            //pas de provider, ni de package, on les crée tous les deux
            Provider newProvider = new Provider(packageKbartDto.getProvider());
            Provider savedProvider = providerRepository.save(newProvider);
            ProviderPackage providerPackage = new ProviderPackage(new ProviderPackageId(packageKbartDto.getPackageName(), packageKbartDto.getDatePackage(), savedProvider.getIdtProvider()));
            return providerPackageRepository.save(providerPackage);
        }
    }
}
