package fr.abes.logskbart.controller;

import fr.abes.logskbart.dto.LigneLogDto;
import fr.abes.logskbart.dto.LogWebDto;
import fr.abes.logskbart.entity.LogKbart;
import fr.abes.logskbart.exception.EmptyFileException;
import fr.abes.logskbart.service.LogsService;
import fr.abes.logskbart.utils.UtilsMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

@Tag(name = "Logskbart-api localhost", description = "Logskbart localhost managements APIs")
@RestController
@CrossOrigin(origins = "*")
@RequestMapping("/v1")
public class LogsController {
    private final LogsService service;


    public LogsController(LogsService service) {
        this.service = service;
    }

    @Operation(
            summary = "Transfer kafka to PostgreSQL DB",
            description = "Retrieves kbart load logs from a Kafka bus and stores them in a DB for later availability",
            responses = {
                    @ApiResponse(responseCode = "200", description = "The request was successful.", content = {@Content(schema = @Schema(implementation = LogWebDto.class), mediaType = "application/json")}),
                    @ApiResponse(responseCode = "400", description = "An element of the query is badly formulated.", content = {@Content(schema = @Schema())}),
                    @ApiResponse(responseCode = "500", description = "An internal server error interrupted processing.", content = {@Content(schema = @Schema())}),
            }
    )
    @GetMapping("/logs/{filename}/{date}")
    public ResponseEntity<InputStreamResource> getLogsFromPackageAndDate(@PathVariable String filename, @PathVariable String date) throws ParseException, IOException, EmptyFileException {
        SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy");
        Date dateAnalyse = format.parse(date);
        File fichier = service.getLogKbartForPackage(filename, dateAnalyse);
        FileInputStream fs = new FileInputStream(fichier);
        // Définir les en-têtes pour la réponse HTTP
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + fichier.getName());
        headers.add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);

        // Retourner la réponse avec le fichier à télécharger
        return ResponseEntity.ok()
                .headers(headers)
                .contentLength(fichier.length())
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(new InputStreamResource(fs));

    }
}
