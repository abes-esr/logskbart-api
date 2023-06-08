package fr.abes.logskbart.utils;

import fr.abes.logskbart.dto.LigneKbartDto;
import fr.abes.logskbart.entity.bacon.LigneKbart;
import org.modelmapper.Converter;
import org.modelmapper.spi.MappingContext;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

import java.text.ParseException;
import java.text.SimpleDateFormat;

@Component
public class LigneKbartMapper {
    private final UtilsMapper mapper;

    public LigneKbartMapper(UtilsMapper mapper) {
        this.mapper = mapper;
    }

    /**
     * Convertion d'une Dto LigneKbartDto en entité LigneKbart
     */
    @Bean
    public void converterLigneKbartDtoToLigneKbart() {
        Converter<LigneKbartDto, LigneKbart> myConverter = new Converter<LigneKbartDto, LigneKbart>() {
            public LigneKbart convert(MappingContext<LigneKbartDto, LigneKbart> context) {
                SimpleDateFormat format = new SimpleDateFormat("dd/MM/yy");
                LigneKbartDto source = context.getSource();
                LigneKbart target = new LigneKbart();
                try {
                    target.setPublicationTitle(source.getPublicationTitle());
                    target.setPrintIdentifier(source.getPrintIdentifier());
                    target.setOnlineIdentifer(source.getOnlineIdentifier());
                    if (source.getDateFirstIssueOnline() != null && !source.getDateFirstIssueOnline().isBlank())
                        target.setDateFirstIssueOnline(format.parse(source.getDateFirstIssueOnline()));
                    target.setNumFirstVolOnline(source.getNumFirstVolOnline() != null ? source.getNumFirstVolOnline().toString() : null);
                    target.setNumFirstIssueOnline(source.getNumFirstIssueOnline() != null ? source.getNumFirstIssueOnline().toString() : null);
                    if (source.getDateLastIssueOnline() != null && !source.getDateLastIssueOnline().isBlank())
                        target.setDateLastIssueOnline(format.parse(source.getDateLastIssueOnline()));
                    target.setNumLastVolOnline(source.getNumLastVolOnline() != null ? source.getNumLastVolOnline().toString() : null);
                    target.setNumlastIssueOnline(source.getNumLastIssueOnline() != null ? source.getNumLastIssueOnline().toString() : null);
                    target.setTitleUrl(source.getTitleUrl());
                    target.setFirstAuthor(source.getFirstAuthor());
                    target.setTitleId(source.getTitleId());
                    target.setEmbargoInfo(source.getEmbargoInfo());
                    target.setCoverageDepth(source.getCoverageDepth());
                    target.setNotes(source.getNotes());
                    target.setPublisherName(source.getPublisherName());
                    target.setPublicationType(source.getPublicationType());
                    if (source.getDateMonographPublishedPrint() != null && !source.getDateMonographPublishedPrint().isBlank())
                        target.setDateMonographPublishedPrint(format.parse(source.getDateMonographPublishedPrint()));
                    if (source.getDateMonographPublishedOnline() != null && !source.getDateMonographPublishedOnline().isBlank())
                        target.setDateMonographPublishedOnline(format.parse(source.getDateMonographPublishedOnline()));
                    target.setMonographVolume(source.getMonographVolume() != null ? source.getMonographVolume().toString() : null);
                    target.setMonographEdition(source.getMonographEdition());
                    target.setFirstEditor(source.getFirstEditor());
                    target.setParentPublicationTitleId(source.getParentPublicationTitleId());
                    target.setPrecedeingPublicationTitleId(source.getPrecedingPublicationTitleId());
                    target.setAccessType(source.getAccessType());
                    target.setBestPpn(source.getBestPpn());
                } catch (ParseException e) {
                    throw new RuntimeException(e);
                }
                return target;
            }
        };
        mapper.addConverter(myConverter);
    }
}
