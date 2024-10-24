package fr.abes.logskbart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpPost;
import fr.abes.logskbart.dto.MailDto;
import lombok.extern.log4j.Log4j2;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Log4j2
@Service
public class EmailService {

    @Value("${mail.ws.recipient}")
    private String recipient;

    @Value("${mail.ws.url}")
    private String url;

    @Value("${spring.profiles.active}")
    private String env;

    public void sendEmail(String packageName, String message) {
        //  Création du mail
        String requestJson = mailToJSON(this.recipient, "[KBART2BACON : erreurs]" + getTag() + " " + packageName, message);

        //  Envoi du message par mail
        sendMail(requestJson);

        log.info("L'email a été correctement envoyé à " + recipient);
    }

    public void sendMailWithAttachment(String packageName, Path mailAttachmentPath) {
        try {
            //  Création du mail
            String requestJson = mailToJSON(this.recipient, "[KBART2BACON : erreurs]" + getTag() + " " + packageName, "/applis/bacon/toLoad/"+mailAttachmentPath.getFileName());

            //  Récupération du fichier
            File file = mailAttachmentPath.toFile();

            //  Envoi du message par mail
            sendMailWithFile(requestJson, file);

            //  Suppression du fichier temporaire
            Files.deleteIfExists(mailAttachmentPath);

            log.info("L'email avec PJ a été correctement envoyé à " + recipient);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void sendMailWithFile(String requestJson, File f) {
        //  Création du l'adresse du ws d'envoi de mails
        HttpPost uploadFile = new HttpPost(this.url + "v2/htmlMailAttachment/");

        //  Création du builder
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addTextBody("mail", requestJson, ContentType.APPLICATION_JSON);

        try {
            builder.addBinaryBody(
                    "attachment",
                    new FileInputStream(f),
                    ContentType.APPLICATION_OCTET_STREAM,
                    f.getName()
            );
        } catch (FileNotFoundException e) {
            log.warn("Le fichier n'a pas été trouvé. " + e.getMessage());
        }

        //  Envoi du mail
        HttpEntity multipart = builder.build();
        uploadFile.setEntity(multipart);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            httpClient.execute(uploadFile);
        } catch (IOException e) {
            log.warn("Erreur lors de l'envoi du mail. " + e.getMessage());
        }
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

    private String getTag(){
        if(env.equalsIgnoreCase("PROD")){
            return "";
        } else {
            return "[" + env.toUpperCase() + "]";
        }
    }
}
