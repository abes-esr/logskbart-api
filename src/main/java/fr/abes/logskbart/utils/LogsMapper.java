package fr.abes.logskbart.utils;

import fr.abes.logskbart.dto.LigneLogDto;
import fr.abes.logskbart.dto.LogKbartDto;
import fr.abes.logskbart.entity.LogKbart;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class LogsMapper {
    private final UtilsMapper mapper;

    public LogsMapper(UtilsMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Convertion d'une Dto InfoBaconDto en modèle LogKbart
     */
    @Bean
    public void converterInfoBaconDtoToLogKbart() {
        Converter<LogKbartDto, LogKbart> myConverter = new Converter<LogKbartDto, LogKbart>() {
            public LogKbart convert(MappingContext<LogKbartDto, LogKbart> context) {
                LogKbartDto source = context.getSource();
                LogKbart target = new LogKbart();
                target.setLevel(Level.valueOf(source.getLevel()));
                target.setMessage(source.getMessage());
                target.setThread(source.getThread());
                target.setLoggerFqcn(source.getLoggerFqcn());
                target.setEndOfBatch(source.isEndOfBatch());
                target.setThreadId(source.getThreadId());
                target.setThreadPriority(source.getThreadPriority());
                target.setLoggerName(source.getLoggerName());
                return target;
            }
        };
        mapper.addConverter(myConverter);
    }

    @Bean
    public void converterLogKbartToLogDto() {
        Converter<LogKbart, LigneLogDto> myConverter = new Converter<LogKbart, LigneLogDto>() {
            public LigneLogDto convert(MappingContext<LogKbart, LigneLogDto> context) {
                LogKbart source = context.getSource();
                LigneLogDto target = new LigneLogDto();
                target.setLevel(source.getLevel().toString());
                target.setMessage(source.getMessage());
                return target;
            }
        };
        mapper.addConverter(myConverter);
    }
}
