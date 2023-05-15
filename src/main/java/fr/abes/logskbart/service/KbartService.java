package fr.abes.logskbart.service;

import fr.abes.logskbart.dto.PackageKbartDto;
import fr.abes.logskbart.utils.LoaderType;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class KbartService {
    private final KbartLoaderFactory factory;

    private final Environment env;

    public KbartService(KbartLoaderFactory factory, Environment env) {
        this.factory = factory;
        this.env = env;
    }

    public void chargerKbart(PackageKbartDto packageKbart) {
        KbartLoader loader = factory.getLoader(LoaderType.valueOf(env.getProperty("loaderType")));
        loader.chargerPackageKbart(packageKbart);
    }
}
