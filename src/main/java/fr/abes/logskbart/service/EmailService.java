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
import org.springframework.stereotype.Service;

import java.io.*;
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

    public void sendMailWithAttachment(String packageName, Path mailAttachmentPath) {
        try {
            //  Création du mail
            String requestJson = mailToJSON(this.recipient, "[CONVERGENCE]["+env.toUpperCase()+"] Log(s) d'erreur de " + packageName + ".csv", "");

            //  Récupération du fichier
            File file = mailAttachmentPath.toFile();

            //  Envoi du message par mail
            sendMailWithFile(requestJson, file);

            //  Suppression du fichier temporaire
            Files.deleteIfExists(mailAttachmentPath);

            log.info("L'email a été correctement envoyé à " + recipient);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected void sendMailWithFile(String requestJson, File f) {
        //  Création du l'adresse du ws d'envoi de mails
        HttpPost uploadFile = new HttpPost(this.url + "htmlMailAttachment/");

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
}
