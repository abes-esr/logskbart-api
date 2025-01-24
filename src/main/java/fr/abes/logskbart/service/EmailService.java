package fr.abes.logskbart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.logskbart.dto.MailDto;
import lombok.extern.log4j.Log4j2;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Log4j2
@Service
public class EmailService {

    @Value("${mail.ws.recipient}")
    private String recipient;

    @Value("${mail.ws.url}")
    private String url;

    @Value("${spring.profiles.active}")
    private String env;

    @Value("${serveur.url}")
    private String serveurUrl;

    public void sendEmail(String packageName) {
        //  Création du mail
        String requestJson = mailToJSON(this.recipient, "[KBART2BACON : erreurs]" + getTag() + " " + packageName, "<a href=\"" + serveurUrl + packageName.replace(".tsv", ".bad") + "\">Cliquez pour télécharger le fichier .bad</a>");

        //  Envoi du message par mail
        sendMail(requestJson);

        log.info("L'email a été correctement envoyé à " + recipient);
    }

    protected void sendMail(String requestJson) {
        RestTemplate restTemplate = new RestTemplate(); //appel ws qui envoie le mail
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestJson, headers);

        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        try {
            restTemplate.postForObject(url + "htmlMail/", entity, String.class); //appel du ws avec
        } catch (Exception e) {
            log.warn("Erreur dans l'envoi du mail d'erreur Sudoc" + e);
        }
        //  Création du l'adresse du ws d'envoi de mails
        HttpPost mail = new HttpPost(this.url + "htmlMail/");

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            httpClient.execute(mail);
        } catch (IOException e) {
            log.warn("Erreur lors de l'envoi du mail. " + e);
        }
    }

    protected String mailToJSON(String to, String subject, String text) {
        String json = "";
        ObjectMapper mapper = new ObjectMapper();
        MailDto mail = new MailDto();
        mail.setApp("convergence");
        mail.setTo(to.split(";"));
        mail.setCc(new String[]{});
        mail.setCci(new String[]{});
        mail.setSubject(subject);
        mail.setText(text);
        try {
            json = mapper.writeValueAsString(mail);
        } catch (JsonProcessingException e) {
            log.warn("Erreur lors de la création du mail. " + e);
        }
        return json;
    }

    private String getTag() {
        if (env.equalsIgnoreCase("PROD")) {
            return "";
        } else {
            return "[" + env.toUpperCase() + "]";
        }
    }
}
