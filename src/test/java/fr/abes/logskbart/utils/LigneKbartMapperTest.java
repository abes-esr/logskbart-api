package fr.abes.logskbart.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import fr.abes.logskbart.dto.LigneKbartDto;
import fr.abes.logskbart.entity.bacon.LigneKbart;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.text.SimpleDateFormat;

@ExtendWith(SpringExtension.class)
@SpringBootTest(classes = {UtilsMapper.class, ObjectMapper.class, LigneKbartMapper.class})
public class LigneKbartMapperTest {
    @Autowired
    UtilsMapper mapper;

    @Test
    @DisplayName("test Mapper ligneKbartDto to Entity : filled dates")
    void testMapperLigneKbart1() {
        SimpleDateFormat format = new SimpleDateFormat("dd/MM/yyyy");
        LigneKbartDto ligne = new LigneKbartDto();
        ligne.setPublicationTitle("testTitre");
        ligne.setPrintIdentifier("978-123456789");
        ligne.setOnlineIdentifier("1234-5678");
        ligne.setDateFirstIssueOnline("01/01/2023");
        ligne.setNumFirstVolOnline(1);
        ligne.setNumFirstIssueOnline(2);
        ligne.setDateLastIssueOnline("02/02/2023");
        ligne.setNumLastVolOnline(3);
        ligne.setNumLastIssueOnline(4);
        ligne.setTitleUrl("testUrl");
        ligne.setFirstAuthor("testAuthor");
        ligne.setTitleId("testId");
        ligne.setEmbargoInfo("testEmbargo");
        ligne.setCoverageDepth("testCoverage");
        ligne.setNotes("notes");
        ligne.setPublisherName("testPublisher");
        ligne.setPublicationType("testType");
        ligne.setDateMonographPublishedPrint("03/03/2023");
        ligne.setDateMonographPublishedOnline("04/04/2023");
        ligne.setMonographVolume(5);
        ligne.setMonographEdition("testEdition");
        ligne.setFirstEditor("testEditor");
        ligne.setParentPublicationTitleId("testParentPub");
        ligne.setPrecedingPublicationTitleId("testPrecedingPub");
        ligne.setAccessType("testAccess");
        ligne.setBestPpn("123456789");

        LigneKbart ligneKbart = mapper.map(ligne, LigneKbart.class);

        Assertions.assertEquals("testTitre", ligneKbart.getPublicationTitle());
        Assertions.assertEquals("978-123456789", ligneKbart.getPrintIdentifier());
        Assertions.assertEquals("1234-5678", ligneKbart.getOnlineIdentifer());
        Assertions.assertEquals("01/01/2023", format.format(ligneKbart.getDateFirstIssueOnline()));
        Assertions.assertEquals("1", ligneKbart.getNumFirstVolOnline());
        Assertions.assertEquals("2", ligneKbart.getNumFirstIssueOnline());
        Assertions.assertEquals("02/02/2023", format.format(ligneKbart.getDateLastIssueOnline()));
        Assertions.assertEquals("3", ligneKbart.getNumLastVolOnline());
        Assertions.assertEquals("4", ligneKbart.getNumlastIssueOnline());
        Assertions.assertEquals("testUrl", ligneKbart.getTitleUrl());
        Assertions.assertEquals("testAuthor", ligneKbart.getFirstAuthor());
        Assertions.assertEquals("testId", ligneKbart.getTitleId());
        Assertions.assertEquals("testEmbargo", ligneKbart.getEmbargoInfo());
        Assertions.assertEquals("testCoverage", ligneKbart.getCoverageDepth());
        Assertions.assertEquals("notes", ligneKbart.getNotes());
        Assertions.assertEquals("testPublisher", ligneKbart.getPublisherName());
        Assertions.assertEquals("testType", ligneKbart.getPublicationType());
        Assertions.assertEquals("03/03/2023", format.format(ligneKbart.getDateMonographPublishedPrint()));
        Assertions.assertEquals("04/04/2023", format.format(ligneKbart.getDateMonographPublishedOnline()));
        Assertions.assertEquals("5", ligneKbart.getMonographVolume());
        Assertions.assertEquals("testEdition", ligneKbart.getMonographEdition());
        Assertions.assertEquals("testParentPub", ligneKbart.getParentPublicationTitleId());
        Assertions.assertEquals("testPrecedingPub", ligneKbart.getPrecedeingPublicationTitleId());
        Assertions.assertEquals("testAccess", ligneKbart.getAccessType());
        Assertions.assertEquals("123456789", ligneKbart.getBestPpn());
    }

    @Test
    @DisplayName("test Mapper ligneKbartDto to Entity : empty dates")
    void testMapperLigneKbart2() {
        LigneKbartDto ligne = new LigneKbartDto();
        ligne.setPublicationTitle("testTitre");
        ligne.setPrintIdentifier("978-123456789");
        ligne.setOnlineIdentifier("1234-5678");
        ligne.setDateFirstIssueOnline("");
        ligne.setNumFirstVolOnline(1);
        ligne.setNumFirstIssueOnline(2);
        ligne.setDateLastIssueOnline("");
        ligne.setNumLastVolOnline(3);
        ligne.setNumLastIssueOnline(4);
        ligne.setTitleUrl("testUrl");
        ligne.setFirstAuthor("testAuthor");
        ligne.setTitleId("testId");
        ligne.setEmbargoInfo("testEmbargo");
        ligne.setCoverageDepth("testCoverage");
        ligne.setNotes("notes");
        ligne.setPublisherName("testPublisher");
        ligne.setPublicationType("testType");
        ligne.setDateMonographPublishedPrint("");
        ligne.setDateMonographPublishedOnline("");
        ligne.setMonographVolume(5);
        ligne.setMonographEdition("testEdition");
        ligne.setFirstEditor("testEditor");
        ligne.setParentPublicationTitleId("testParentPub");
        ligne.setPrecedingPublicationTitleId("testPrecedingPub");
        ligne.setAccessType("testAccess");
        ligne.setBestPpn("123456789");

        LigneKbart ligneKbart = mapper.map(ligne, LigneKbart.class);

        Assertions.assertEquals("testTitre", ligneKbart.getPublicationTitle());
        Assertions.assertEquals("978-123456789", ligneKbart.getPrintIdentifier());
        Assertions.assertEquals("1234-5678", ligneKbart.getOnlineIdentifer());
        Assertions.assertNull(ligneKbart.getDateFirstIssueOnline());
        Assertions.assertEquals("1", ligneKbart.getNumFirstVolOnline());
        Assertions.assertEquals("2", ligneKbart.getNumFirstIssueOnline());
        Assertions.assertNull(ligneKbart.getDateLastIssueOnline());
        Assertions.assertEquals("3", ligneKbart.getNumLastVolOnline());
        Assertions.assertEquals("4", ligneKbart.getNumlastIssueOnline());
        Assertions.assertEquals("testUrl", ligneKbart.getTitleUrl());
        Assertions.assertEquals("testAuthor", ligneKbart.getFirstAuthor());
        Assertions.assertEquals("testId", ligneKbart.getTitleId());
        Assertions.assertEquals("testEmbargo", ligneKbart.getEmbargoInfo());
        Assertions.assertEquals("testCoverage", ligneKbart.getCoverageDepth());
        Assertions.assertEquals("notes", ligneKbart.getNotes());
        Assertions.assertEquals("testPublisher", ligneKbart.getPublisherName());
        Assertions.assertEquals("testType", ligneKbart.getPublicationType());
        Assertions.assertNull(ligneKbart.getDateMonographPublishedPrint());
        Assertions.assertNull(ligneKbart.getDateMonographPublishedOnline());
        Assertions.assertEquals("5", ligneKbart.getMonographVolume());
        Assertions.assertEquals("testEdition", ligneKbart.getMonographEdition());
        Assertions.assertEquals("testParentPub", ligneKbart.getParentPublicationTitleId());
        Assertions.assertEquals("testPrecedingPub", ligneKbart.getPrecedeingPublicationTitleId());
        Assertions.assertEquals("testAccess", ligneKbart.getAccessType());
        Assertions.assertEquals("123456789", ligneKbart.getBestPpn());
    }
}
