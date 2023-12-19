package fr.abes.logskbart.dto;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class LigneLogDto {
    private String level;
    private String message;
    private Integer nbLine;
}
