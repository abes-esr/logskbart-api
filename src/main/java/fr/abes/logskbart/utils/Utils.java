package fr.abes.logskbart.utils;

import fr.abes.logskbart.exception.IllegalDateException;
import fr.abes.logskbart.exception.IllegalPackageException;
import fr.abes.logskbart.exception.IllegalProviderException;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Utils {
    public static String extractProvider(String filename) throws IllegalProviderException {
        try {
            return filename.substring(0, filename.indexOf('_'));
        } catch (Exception e) {
            throw new IllegalProviderException(e);
        }
    }

    public static String extractPackageName(String filename) throws IllegalPackageException {
        try {
            return filename.substring(filename.indexOf('_') + 1, filename.lastIndexOf('_'));
        } catch (Exception e) {
            throw new IllegalPackageException(e);
        }
    }

    public static Date extractDate(String filename) throws IllegalDateException {
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
        try {
            return format.parse(filename.substring(filename.lastIndexOf('_') + 1, filename.lastIndexOf(".tsv")));
        } catch (Exception e) {
            throw new IllegalDateException(e);
        }
    }
}
