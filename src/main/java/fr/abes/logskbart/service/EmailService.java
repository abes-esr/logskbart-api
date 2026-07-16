package fr.abes.logskbart.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.logskbart.dto.MailDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.StringHttpMessageConverter;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;
import org.springframework.web.util.UriUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

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

    public void sendOtherErrorsEmail(String packageName) {
        String safePackageName = Path.of(packageName).getFileName().toString();
        String reportName = safePackageName.replaceFirst("(?i)\\.tsv$", "_other.bad");
        String reportUrl = reportUrl(reportName);
        String requestJson = mailToJSON(
                this.recipient,
                "[KBART2BACON : erreurs hors 400]" + getTag() + " " + safePackageName,
                "<a href=\"" + reportUrl + "\" target=\"_blank\">Cliquez pour télécharger "
                        + HtmlUtils.htmlEscape(reportName) + "</a>"
        );

        if (sendMail(requestJson)) {
            log.info("L'email des erreurs hors 400 a été correctement envoyé à {}", recipient);
        }
    }

    public boolean sendDailyRecapEmail(List<String> filenames) {
        StringBuilder links = new StringBuilder("<p>Rapports d'erreurs 400 à traiter :</p>");
        filenames.stream()
                .distinct()
                .sorted()
                .forEach(filename -> links
                        .append("<a href=\"")
                        .append(reportUrl(filename))
                        .append("\" target=\"_blank\">")
                        .append(HtmlUtils.htmlEscape(filename))
                        .append("</a><br/>"));

        String requestJson = mailToJSON(
                this.recipient,
                "[KBART2BACON : récapitulatif erreurs 400]" + getTag(),
                links.toString()
        );
        boolean sent = sendMail(requestJson);
        if (sent) {
            log.info("L'email quotidien des erreurs 400 a été correctement envoyé à {}", recipient);
        }
        return sent;
    }

    public void sendCandidatsDoublonsEmail(String packageName) {
        //  Création du mail avec lien vers le fichier CandidatsDoublons.txt
        String requestJson = mailToJSON(this.recipient, "[KBART2BACON : Candidats Doublons]" + getTag() + " " + packageName, "<a href=\"" + serveurUrl + "CandidatsDoublons.txt" + "\" target=\"_blank\">Cliquez pour télécharger le fichier CandidatsDoublons.txt</a>");

        //  Envoi du message par mail
        sendMail(requestJson);

        log.info("L'email CandidatsDoublons a été correctement envoyé à " + recipient);
    }

    protected boolean sendMail(String requestJson) {
        RestTemplate restTemplate = new RestTemplate(); //appel ws qui envoie le mail
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestJson, headers);

        restTemplate.getMessageConverters()
                .add(0, new StringHttpMessageConverter(StandardCharsets.UTF_8));

        try {
            restTemplate.postForObject(url + "htmlMail/", entity, String.class); //appel du ws avec
            return true;
        } catch (Exception e) {
            log.warn("Erreur dans l'envoi du mail d'erreur Sudoc" + e);
            return false;
        }
    }

    private String reportUrl(String filename) {
        String baseUrl = serveurUrl.endsWith("/") ? serveurUrl : serveurUrl + "/";
        return baseUrl + "bad/" + UriUtils.encodePathSegment(filename, StandardCharsets.UTF_8);
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
