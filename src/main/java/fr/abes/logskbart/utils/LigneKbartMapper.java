package fr.abes.logskbart.utils;

import fr.abes.logskbart.dto.Kbart2KafkaDto;
import fr.abes.logskbart.dto.LigneKbartDto;
import fr.abes.logskbart.entity.bacon.LigneKbart;
import fr.abes.logskbart.entity.logs.LogKbart;
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
                    target.setPublicationTitle(source.getPublication_title());
                    target.setPrintIdentifier(source.getPrint_identifier());
                    target.setOnlineIdentifer(source.getOnline_identifier());
                    target.setDateFirstIssueOnline(source.getDate_first_issue_online() != null ? format.parse(source.getDate_first_issue_online()) : null);
                    target.setNumFirstVolOnline(source.getNum_first_vol_online() != null ? source.getNum_first_vol_online().toString() : null);
                    target.setNumFirstIssueOnline(source.getNum_first_issue_online() != null ? source.getNum_first_issue_online().toString() : null);
                    target.setDateLastIssueOnline(source.getDate_last_issue_online() != null ? format.parse(source.getDate_last_issue_online()) : null);
                    target.setNumLastVolOnline(source.getNum_last_vol_online() != null ? source.getNum_last_vol_online().toString() : null);
                    target.setNumlastIssueOnline(source.getNum_last_issue_online() != null ? source.getNum_last_issue_online().toString() : null);
                    target.setTitleUrl(source.getTitle_url());
                    target.setFirstAuthor(source.getFirst_author());
                    target.setTitleId(source.getTitle_id());
                    target.setEmbargoInfo(source.getEmbargo_info());
                    target.setCoverageDepth(source.getCoverage_depth());
                    target.setNotes(source.getNotes());
                    target.setPublisherName(source.getPublisher_name());
                    target.setPublicationType(source.getPublication_type());
                    target.setDateMonographPublishedPrint(source.getDate_monograph_published_print() != null ? format.parse(source.getDate_monograph_published_print()) : null);
                    target.setDateMonographPublishedOnline(source.getDate_monograph_published_online() != null ? format.parse(source.getDate_monograph_published_online()) : null);
                    target.setMonographVolume(source.getMonograph_volume() != null ? source.getMonograph_volume().toString() : null);
                    target.setMonographEdition(source.getMonograph_edition());
                    target.setFirstEditor(source.getFirst_editor());
                    target.setParentPublicationTitleId(source.getParent_publication_title_id());
                    target.setPrecedeingPublicationTitleId(source.getPreceding_publication_title_id());
                    target.setAccessType(source.getAccess_type());
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
