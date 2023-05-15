package fr.abes.logskbart.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class PackageKbartDto {
    private List<LigneKbartDto> ligneKbartDtos;
}
