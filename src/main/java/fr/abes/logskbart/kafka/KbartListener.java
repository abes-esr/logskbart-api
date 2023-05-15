package fr.abes.logskbart.kafka;

import fr.abes.logskbart.service.KbartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class KbartListener {
    @Autowired
    private KbartService service;


}
