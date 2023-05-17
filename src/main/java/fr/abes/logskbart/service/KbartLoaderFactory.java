package fr.abes.logskbart.service;

import fr.abes.logskbart.utils.LoaderType;
import org.springframework.stereotype.Service;

@Service
public class KbartLoaderFactory {
    private final KbartToTsvService kbartToTsvService;
    private final KbartToBddService kbartToBddService;

    public KbartLoaderFactory(KbartToTsvService kbartToTsvService, KbartToBddService kbartToBddService) {
        this.kbartToTsvService = kbartToTsvService;
        this.kbartToBddService = kbartToBddService;
    }

    public KbartLoader getLoader(LoaderType type) {
        return switch (type) {
            case tsv -> kbartToTsvService;
            case bdd -> kbartToBddService;
        };
    }
}
