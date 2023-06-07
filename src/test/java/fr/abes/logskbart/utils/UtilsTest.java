package fr.abes.logskbart.utils;

import fr.abes.logskbart.exception.IllegalDateException;
import fr.abes.logskbart.exception.IllegalPackageException;
import fr.abes.logskbart.exception.IllegalProviderException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.text.SimpleDateFormat;


class UtilsTest {

    @Test
    void extractProvider() throws IllegalProviderException {
        String filename = "RSC_GLOBAL_ALLEBOOKS_2021-01-28.tsv";
        Assertions.assertEquals("RSC", Utils.extractProvider(filename));

        String filename2 = "";
        Assertions.assertThrows(IllegalProviderException.class, () -> Utils.extractProvider(filename2));
    }

    @Test
    void extractPackageName() throws IllegalPackageException {
        String filename = "RSC_GLOBAL_ALLEBOOKS_2021-01-28.tsv";
        Assertions.assertEquals("GLOBAL_ALLEBOOKS", Utils.extractPackageName(filename));

        String filename2 = "";
        Assertions.assertThrows(IllegalPackageException.class, () -> Utils.extractPackageName(filename2));
    }

    @Test
    void extractDate() throws IllegalDateException {
        String filename = "RSC_GLOBAL_ALLEBOOKS_2021-01-28.tsv";
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yy");
        Assertions.assertEquals("28/01/21", format.format(Utils.extractDate(filename)));

        String filename2 = "";
        Assertions.assertThrows(IllegalDateException.class, () -> Utils.extractDate(filename2));

        String filename3 = "RSC_GLOBAL_ALLEBOOKS_2021048.tsv";
        Assertions.assertThrows(IllegalDateException.class, () -> Utils.extractDate(filename3));
    }
}