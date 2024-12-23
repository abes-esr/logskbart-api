package fr.abes.logskbart.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class LogWebDto {
    public String filename;
    public String date;
    List<LigneLogDto> ligneLogs;

    public LogWebDto(String filename, String date) {
        this.filename = filename;
        this.date = date;
        this.ligneLogs = new ArrayList<>();
    }

    public void addLignes(List<LigneLogDto> lignes) {
        this.ligneLogs.addAll(lignes);
    }
}
