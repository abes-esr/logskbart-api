package fr.abes.logskbart.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class PackageKbartDtoKafka {
    @JsonProperty("kbartDtos")
    private List<LigneKbartDto> kbartDtos;

    public PackageKbartDtoKafka() {
        this.kbartDtos = new ArrayList<>();
    }
}
