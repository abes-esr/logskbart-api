package fr.abes.logskbart.utils;

import fr.abes.logskbart.dto.Bacon2KafkaDto;
import fr.abes.logskbart.dto.LigneLogDto;
import fr.abes.logskbart.dto.LogDto;
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
     * Convertion d'un modèle PresenceZoneWebDto en modèle ComplexRule
     */
    @Bean
    public void converterInfoBaconDtoToLogKbart() {
        Converter<Bacon2KafkaDto, LogKbart> myConverter = new Converter<Bacon2KafkaDto, LogKbart>() {
            public LogKbart convert(MappingContext<Bacon2KafkaDto, LogKbart> context) {
                Bacon2KafkaDto source = context.getSource();
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