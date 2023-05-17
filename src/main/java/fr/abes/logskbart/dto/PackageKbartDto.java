package fr.abes.logskbart.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PackageKbartDto {
    private String packageName;
    private List<LigneKbartDto> ligneKbartDtos = new ArrayList<>();

    public void addLigneKbart(LigneKbartDto ligne) {
        this.ligneKbartDtos.add(ligne);
    }
}
