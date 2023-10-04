package fr.abes.logskbart.service;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;
import com.opencsv.bean.StatefulBeanToCsv;
import com.opencsv.bean.StatefulBeanToCsvBuilder;
import com.opencsv.exceptions.CsvDataTypeMismatchException;
import com.opencsv.exceptions.CsvRequiredFieldEmptyException;
import fr.abes.logskbart.dto.LigneKbartDto;
import fr.abes.logskbart.dto.PackageKbartDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.stream.Collectors;

@Service
public class KbartToTsvService implements KbartLoader {

    // TODO supprimer cette classe

    @Value("${path.tsvFile}")
    private String pathToFichier;

    @Override
    public void chargerPackageKbart(PackageKbartDto packageKbartDto) {
        try (Writer writer = new FileWriter(pathToFichier + packageKbartDto.getPackageName())) {
            writer.append(buildHeader(LigneKbartDto.class));
            StatefulBeanToCsv<LigneKbartDto> sbc = new StatefulBeanToCsvBuilder<LigneKbartDto>(writer).withSeparator('\t').withApplyQuotesToAll(false).build();
            sbc.write(packageKbartDto.getLigneKbartDtos());
        } catch (IOException | CsvRequiredFieldEmptyException | CsvDataTypeMismatchException e) {
            throw new RuntimeException(e);
        }

    }

    private String buildHeader(Class<LigneKbartDto> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .filter(f -> f.getAnnotation(CsvBindByPosition.class) != null
                        && f.getAnnotation(CsvBindByName.class) != null)
                .sorted(Comparator.comparing(f -> f.getAnnotation(CsvBindByPosition.class).position()))
                .map(f -> f.getAnnotation(CsvBindByName.class).column())
                .collect(Collectors.joining("\t")) + "\n";
    }

}
