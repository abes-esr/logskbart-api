package fr.abes.logskbart.controller;

import fr.abes.logskbart.dto.LigneLogDto;
import fr.abes.logskbart.dto.LogDto;
import fr.abes.logskbart.entity.LogKbart;
import fr.abes.logskbart.service.LogsService;
import fr.abes.logskbart.utils.UtilsMapper;
import jdk.jshell.execution.Util;
import org.springframework.web.bind.annotation.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
