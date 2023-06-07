package fr.abes.logskbart.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.logskbart.dto.LigneKbartDto;
import fr.abes.logskbart.dto.PackageKbartDto;
import fr.abes.logskbart.exception.IllegalDateException;
import fr.abes.logskbart.exception.IllegalPackageException;
import fr.abes.logskbart.exception.IllegalProviderException;
import fr.abes.logskbart.service.KbartService;
import fr.abes.logskbart.utils.Utils;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;


@Service
@Slf4j
public class KbartListener {
    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private KbartService service;

    @KafkaListener(topics = {"packageKbart"}, groupId = "lignesKbart", containerFactory = "kafkaKbartListenerContainerFactory")
    public void listenKbartFromKafka(List<ConsumerRecord<String, String>> lignesKbart) {
        PackageKbartDto packageKbartDto = new PackageKbartDto();
        try {
            packageKbartDto.setPackageName(Utils.extractPackageName(lignesKbart.get(0).key()));
            packageKbartDto.setProvider(Utils.extractProvider(lignesKbart.get(0).key()));
            packageKbartDto.setDatePackage(Utils.extractDate(lignesKbart.get(0).key()));
            //chargement des records issus de kafka dans une liste de DTO
            for (ConsumerRecord<String, String> ligneKbart : lignesKbart) {
                LigneKbartDto kbartDto = null;
                try {
                    kbartDto = mapper.readValue(ligneKbart.value(), LigneKbartDto.class);
                } catch (JsonProcessingException e) {
                    log.error(e.getMessage());
                }
                packageKbartDto.addLigneKbart(kbartDto);
            }
            //traitement de la liste de dto à traiter en fonction
            service.chargerKbart(packageKbartDto);
        } catch (IllegalPackageException | IllegalProviderException | IllegalDateException e) {
            log.error("Erreur dans les données en entrée, provider / nom de package ou format de date incorrect");
        }
    }





}
