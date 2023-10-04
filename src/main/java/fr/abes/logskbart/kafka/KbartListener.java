package fr.abes.logskbart.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.logskbart.dto.PackageKbartDto;
import fr.abes.logskbart.dto.PackageKbartDtoKafka;
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


@Service
@Slf4j
public class KbartListener {

    //  TODO Supprimer ce listener

    @Autowired
    private ObjectMapper mapper;

    @Autowired
    private KbartService service;

    @KafkaListener(topics = {"packageKbart"}, groupId = "lignesKbart", containerFactory = "kafkaKbartListenerContainerFactory")
    public void listenKbartFromKafka(ConsumerRecord<String, String> lignesKbart) {
        PackageKbartDto packageKbartDto = new PackageKbartDto();
        try {
            packageKbartDto.setPackageName(Utils.extractPackageName(lignesKbart.key()));
            packageKbartDto.setProvider(Utils.extractProvider(lignesKbart.key()));
            packageKbartDto.setDatePackage(Utils.extractDate(lignesKbart.key()));
            PackageKbartDtoKafka packageFromKafka = mapper.readValue(lignesKbart.value(), PackageKbartDtoKafka.class);
            packageKbartDto.setLigneKbartDtos(packageFromKafka.getKbartDtos());

            //traitement de la liste de dto à traiter en fonction
            service.chargerKbart(packageKbartDto);
        } catch (IllegalPackageException | IllegalProviderException | IllegalDateException e) {
            log.error("Erreur dans les données en entrée, provider / nom de package ou format de date incorrect");
        } catch (JsonProcessingException e) {
            log.error(e.getMessage());
        }
    }


}
