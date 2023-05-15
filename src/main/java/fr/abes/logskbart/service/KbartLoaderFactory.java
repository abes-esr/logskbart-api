package fr.abes.logskbart.service;

import fr.abes.logskbart.utils.LoaderType;
import org.springframework.stereotype.Service;

@Service
public class KbartLoaderFactory {
    private final KbartToCsvService kbartToCsvService;
    private final KbartToBddService kbartToBddService;

    public KbartLoaderFactory(KbartToCsvService kbartToCsvService, KbartToBddService kbartToBddService) {
        this.kbartToCsvService = kbartToCsvService;
        this.kbartToBddService = kbartToBddService;
    }

    public KbartLoader getLoader(LoaderType type) {
        return switch (type) {
            case csv -> kbartToCsvService;
            case bdd -> kbartToBddService;
        };
    }
}
