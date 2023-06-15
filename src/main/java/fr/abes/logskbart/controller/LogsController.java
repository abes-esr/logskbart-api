package fr.abes.logskbart.controller;

import fr.abes.logskbart.dto.LigneLogDto;
import fr.abes.logskbart.dto.LogDto;
import fr.abes.logskbart.service.LogsService;
import fr.abes.logskbart.utils.UtilsMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

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

    private final UtilsMapper mapper;

    public LogsController(LogsService service, UtilsMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @Operation(
            summary = "Transfer kafka to PostgreSQL DB",
            description = "Retrieves kbart load logs from a Kafka bus and stores them in a DB for later availability",
            responses = {
                    @ApiResponse( responseCode = "200", description = "The request was successful.", content = { @Content(schema = @Schema(implementation = LogDto.class), mediaType = "application/json") } ),
                    @ApiResponse( responseCode = "400", description = "An element of the query is badly formulated.", content = { @Content(schema = @Schema()) } ),
                    @ApiResponse( responseCode = "500", description = "An internal server error interrupted processing.", content = { @Content(schema = @Schema()) } ),
            }
    )
    @GetMapping("/logs/{filename}/{date}")
    public LogDto getLogsFromPackageAndDate(@PathVariable String filename, @PathVariable String date) throws ParseException {
        SimpleDateFormat format = new SimpleDateFormat("ddMMyyyy");
        Date dateAnalyse = format.parse(date);
        LogDto logDto = new LogDto(filename, date);
        List<LigneLogDto> lignes = mapper.mapList(service.getLogKbartForPackage(filename, dateAnalyse), LigneLogDto.class);
        logDto.addLignes(lignes);
        return logDto;
    }
}
